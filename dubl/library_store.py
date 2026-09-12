"""Local character library for Dubl.

The UI treats character files as an internal implementation detail.  Each
character is stored as a normal Dubl JSON document under the application's data
directory, so existing save/load code and backups keep working.
"""
from __future__ import annotations

import copy
import json
import os
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from .model import atomic_save, data_dir, fresh, load_file, normalize


@dataclass(frozen=True)
class CharacterSummary:
    id: str
    name: str
    concept: str
    portrait: str
    updated: float
    xp_total: int
    creation_xp: int
    path: Path


class CharacterStore:
    """Small persistent library with no Qt dependency."""

    def __init__(self, root: Path | None = None):
        self.root = Path(root) if root is not None else data_dir()
        self.characters_dir = self.root / "characters"
        self.settings_path = self.root / "library.json"
        self.characters_dir.mkdir(parents=True, exist_ok=True)
        self._settings = self._read_settings()

    def _read_settings(self) -> dict:
        try:
            data = json.loads(self.settings_path.read_text(encoding="utf-8"))
            return data if isinstance(data, dict) else {}
        except (OSError, ValueError, UnicodeError):
            return {}

    def _write_settings(self) -> None:
        self.root.mkdir(parents=True, exist_ok=True)
        payload = json.dumps(self._settings, ensure_ascii=False, indent=2).encode("utf-8")
        tmp = self.settings_path.with_suffix(".tmp")
        tmp.write_bytes(payload)
        os.replace(tmp, self.settings_path)

    @property
    def last_character(self) -> str | None:
        value = self._settings.get("lastCharacter")
        return str(value) if value else None

    def set_last_character(self, character_id: str | None) -> None:
        if character_id:
            self._settings["lastCharacter"] = str(character_id)
        else:
            self._settings.pop("lastCharacter", None)
        self._write_settings()

    def character_path(self, character_id: str) -> Path:
        # IDs originate from UUIDs in our own model.  Still sanitize anything
        # loaded from external files before using it as a filename.
        safe = "".join(c for c in str(character_id) if c.isalnum() or c in "-_")
        if not safe:
            safe = str(uuid.uuid4())
        return self.characters_dir / f"{safe}.json"

    def exists(self, character_id: str) -> bool:
        return self.character_path(character_id).exists()

    def _summary_from_path(self, path: Path) -> CharacterSummary | None:
        try:
            raw = json.loads(path.read_text(encoding="utf-8-sig"))
            if not isinstance(raw, dict):
                return None
            profile = raw.get("profile", {}) if isinstance(raw.get("profile"), dict) else {}
            cid = str(raw.get("id") or path.stem)
            return CharacterSummary(
                id=cid,
                name=str(profile.get("name") or "Новый персонаж"),
                concept=str(profile.get("concept") or ""),
                portrait=str(profile.get("portrait") or ""),
                updated=path.stat().st_mtime,
                xp_total=int(profile.get("xpTotal") or 0),
                creation_xp=int(profile.get("creationXp") or 0),
                path=path,
            )
        except (OSError, ValueError, TypeError, UnicodeError):
            return None

    def list(self) -> list[CharacterSummary]:
        entries: list[CharacterSummary] = []
        for path in self.characters_dir.glob("*.json"):
            summary = self._summary_from_path(path)
            if summary:
                entries.append(summary)
        entries.sort(key=lambda x: x.updated, reverse=True)
        return entries

    def load(self, character_id: str) -> dict:
        return load_file(self.character_path(character_id))

    def save(self, state: dict) -> Path:
        state = normalize(state)
        cid = str(state.get("id") or uuid.uuid4())
        state["id"] = cid
        path = self.character_path(cid)
        atomic_save(path, state)
        self.set_last_character(cid)
        return path

    def create(self, state: dict | None = None) -> str:
        character = normalize(copy.deepcopy(state)) if state is not None else fresh()
        cid = str(character.get("id") or uuid.uuid4())
        if self.exists(cid):
            cid = str(uuid.uuid4())
            character["id"] = cid
        self.save(character)
        return cid

    def import_file(self, source: str | Path) -> str:
        source = Path(source)
        character = load_file(source)
        cid = str(character.get("id") or uuid.uuid4())
        if self.exists(cid):
            cid = str(uuid.uuid4())
            character["id"] = cid
        if not character["profile"].get("name"):
            character["profile"]["name"] = source.stem
        self.save(character)
        return cid

    def duplicate(self, character_id: str) -> str:
        character = copy.deepcopy(self.load(character_id))
        character["id"] = str(uuid.uuid4())
        base = character.get("profile", {}).get("name") or "Новый персонаж"
        character["profile"]["name"] = f"{base} — копия"
        self.save(character)
        return character["id"]

    def delete(self, character_id: str) -> None:
        path = self.character_path(character_id)
        for candidate in (path, path.with_suffix(path.suffix + ".bak")):
            try:
                candidate.unlink()
            except FileNotFoundError:
                pass
        if self.last_character == character_id:
            remaining = self.list()
            self.set_last_character(remaining[0].id if remaining else None)

    def export(self, character_id: str, destination: str | Path) -> Path:
        destination = Path(destination)
        state = self.load(character_id)
        atomic_save(destination, state)
        return destination

    def migrate_legacy_autosave(self) -> str | None:
        """Bring the pre-library autosave into the library once.

        We only auto-import it when the library is still empty.  The old file is
        deliberately left untouched as an additional recovery copy.
        """
        if self._settings.get("legacyAutosaveChecked"):
            return None
        imported: str | None = None
        legacy = self.root / "autosave.json"
        if not self.list() and legacy.exists():
            try:
                state = load_file(legacy)
                imported = self.create(state)
            except Exception:
                imported = None
        self._settings["legacyAutosaveChecked"] = True
        if imported:
            self._settings["lastCharacter"] = imported
        self._write_settings()
        return imported
