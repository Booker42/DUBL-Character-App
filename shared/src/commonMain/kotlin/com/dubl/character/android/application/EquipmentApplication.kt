package com.dubl.character.android.application

import com.dubl.character.android.model.GearCatalogEntry
import com.dubl.character.android.model.GearItem
import com.dubl.character.android.state.CharacterSession

class EquipmentApplication internal constructor(
    private val session: CharacterSession,
) {
    fun setLoadAutomatic(enabled: Boolean) = session.setGearLoadAutomatic(enabled)
    fun setManualLoad(value: Double) = session.setGearManualLoad(value)
    fun syncCatalogLoads(entries: List<GearCatalogEntry>) = session.syncCatalogGearLoads(entries)
    fun addCatalog(entry: GearCatalogEntry): String = session.addCatalogGear(entry)
    fun addCustom(item: GearItem): String = session.addCustomGear(item)
    fun updateItem(item: GearItem) = session.replaceGearItem(item)
    fun setItemCarried(uid: String, carried: Boolean) = session.setGearItemCarried(uid, carried)
    fun setItemQuantity(uid: String, quantity: Int) = session.setGearItemQuantity(uid, quantity)
    fun removeItem(uid: String) = session.removeGearItem(uid)
}
