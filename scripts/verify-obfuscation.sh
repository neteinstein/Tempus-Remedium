#!/usr/bin/env bash
#
# Asserts that the `release` build really is obfuscated, and that what must survive R8 did.
# Run after `./gradlew assembleRelease`; used by the "Minified Release" job in
# .github/workflows/pr.yml and by .github/workflows/release.yml.
#
# Why: isMinifyEnabled/isShrinkResources (androidApp/build.gradle.kts) and the rules in
# androidApp/proguard-rules.pro can regress silently (a stray blanket -keep, minification switched
# off) and the build still succeeds. Only the mapping file shows what R8 actually did.
set -euo pipefail

readonly APP_PACKAGE_PREFIX="org.mariacf.tempusremedium."

# Classes the system instantiates by the name written in the manifest. AGP keeps them; if one is
# renamed the app crashes on launch, on a reminder or when a widget updates.
readonly KEPT_CLASSES=(
    "org.mariacf.tempusremedium.android.TempusRemediumApplication"
    "org.mariacf.tempusremedium.android.MainActivity"
    "org.mariacf.tempusremedium.android.reminders.ReminderReceiver"
    "org.mariacf.tempusremedium.android.reminders.RescheduleReceiver"
    "org.mariacf.tempusremedium.android.widgets.HojeWidgetReceiver"
    "org.mariacf.tempusremedium.android.widgets.ReporEmBreveWidgetReceiver"
)

# The stored document is read through this generated serializer. If R8 removed it, every launch
# would fail to read the user's data.
readonly PRESENT_CLASSES=(
    "org.mariacf.tempusremedium.core.model.AppData\$\$serializer"
)

failures=0

fail() {
    echo "::error::$1"
    failures=$((failures + 1))
}

# A class line in mapping.txt is unindented: "<original> -> <obfuscated>:". Members are indented,
# so anchoring on the package prefix at the start of the line matches classes only.
count_renamed_classes() {
    awk -F' -> ' -v prefix="$1" '
        index($0, prefix) == 1 && NF == 2 {
            obfuscated = $2
            sub(/:$/, "", obfuscated)
            if (obfuscated != $1) renamed++
        }
        END { print renamed + 0 }
    ' "$2"
}

verify_mapping() {
    local mapping="$1"
    local variant
    variant="$(basename "$(dirname "$mapping")")"

    local renamed
    renamed="$(count_renamed_classes "$APP_PACKAGE_PREFIX" "$mapping")"
    if [ "$renamed" -eq 0 ]; then
        fail "$variant: no ${APP_PACKAGE_PREFIX}* class was renamed - the release build is not obfuscated. Check isMinifyEnabled in androidApp/build.gradle.kts and for an over-broad -keep in androidApp/proguard-rules.pro."
    else
        echo "  $variant: $renamed obfuscated app classes"
    fi

    local kept
    for kept in "${KEPT_CLASSES[@]}"; do
        if grep -qxF "$kept -> $kept:" "$mapping"; then
            echo "  $variant: kept $kept"
        else
            fail "$variant: $kept was renamed or removed - the system cannot instantiate it from the manifest."
        fi
    done

    local present
    for present in "${PRESENT_CLASSES[@]}"; do
        if grep -qF "$present -> " "$mapping"; then
            echo "  $variant: present $present"
        else
            fail "$variant: $present was removed - stored data could not be read. Check kotlinx.serialization's R8 rules."
        fi
    done
}

mappings=()
while IFS= read -r line; do
    mappings+=("$line")
done < <(find androidApp/build/outputs/mapping -mindepth 2 -maxdepth 2 -name mapping.txt -path '*elease/*' | sort)

if [ ${#mappings[@]} -eq 0 ]; then
    echo "::error::No release mapping.txt found under androidApp/build/outputs/mapping/. Run './gradlew assembleRelease' first; if it did run, R8 produced no mapping, which means the build is not being minified."
    exit 1
fi

for mapping in "${mappings[@]}"; do
    verify_mapping "$mapping"
done

if [ "$failures" -ne 0 ]; then
    echo "::error::Obfuscation verification failed with $failures problem(s)."
    exit 1
fi

echo "Obfuscation verified across ${#mappings[@]} release variant(s)."
