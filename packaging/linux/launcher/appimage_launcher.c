#define _GNU_SOURCE
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#ifndef DUBL_PORTABLE_VERSION
#define DUBL_PORTABLE_VERSION "0.2.0"
#endif

extern const unsigned char _binary_payload_tar_start[];
extern const unsigned char _binary_payload_tar_end[];

static int ensure_dir(const char *path) {
    char tmp[PATH_MAX];
    size_t len = strlen(path);
    if (len >= sizeof(tmp)) return -1;
    memcpy(tmp, path, len + 1);
    for (char *p = tmp + 1; *p; ++p) {
        if (*p == '/') {
            *p = '\0';
            if (mkdir(tmp, 0755) != 0 && errno != EEXIST) return -1;
            *p = '/';
        }
    }
    if (mkdir(tmp, 0755) != 0 && errno != EEXIST) return -1;
    return 0;
}

static int write_payload(const char *path) {
    FILE *f = fopen(path, "wb");
    if (!f) return -1;
    const unsigned char *start = _binary_payload_tar_start;
    size_t size = (size_t)(_binary_payload_tar_end - _binary_payload_tar_start);
    size_t written = fwrite(start, 1, size, f);
    int ok = (written == size && fclose(f) == 0) ? 0 : -1;
    return ok;
}

int main(int argc, char **argv) {
    const char *home = getenv("HOME");
    if (!home || !*home) home = "/tmp";

    char base[PATH_MAX];
    snprintf(base, sizeof(base), "%s/.cache/dubl-character/portable-%s", home, DUBL_PORTABLE_VERSION);
    if (ensure_dir(base) != 0) {
        fprintf(stderr, "DUBL: cannot create cache directory: %s\n", base);
        return 2;
    }

    char marker[PATH_MAX];
    char tar_path[PATH_MAX];
    char app_path[PATH_MAX];
    snprintf(marker, sizeof(marker), "%s/.ready", base);
    snprintf(tar_path, sizeof(tar_path), "%s/payload.tar", base);
    snprintf(app_path, sizeof(app_path), "%s/app/bin/dubl", base);

    if (access(marker, F_OK) != 0 || access(app_path, X_OK) != 0) {
        if (write_payload(tar_path) != 0) {
            fprintf(stderr, "DUBL: cannot unpack embedded payload\n");
            return 3;
        }
        char command[PATH_MAX * 2];
        snprintf(command, sizeof(command), "tar -xf '%s' -C '%s'", tar_path, base);
        if (system(command) != 0) {
            fprintf(stderr, "DUBL: system tar failed while unpacking payload\n");
            return 4;
        }
        unlink(tar_path);
        FILE *m = fopen(marker, "w");
        if (m) { fprintf(m, "DUBL %s\n", DUBL_PORTABLE_VERSION); fclose(m); }
    }

    char **child_argv = calloc((size_t)argc + 1, sizeof(char*));
    if (!child_argv) return 5;
    child_argv[0] = app_path;
    for (int i = 1; i < argc; ++i) child_argv[i] = argv[i];
    child_argv[argc] = NULL;

    execv(app_path, child_argv);
    fprintf(stderr, "DUBL: cannot launch %s: %s\n", app_path, strerror(errno));
    return 6;
}
