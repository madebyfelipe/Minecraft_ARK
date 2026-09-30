#!/usr/bin/env bash
# Copia o jar compilado para a instância de teste do Prism Launcher.
#
# A instância usa uma CÓPIA, nunca um atalho para build/libs: recompilar com o jogo
# aberto trocaria o jar debaixo do jogo em execução e quebraria o carregamento de
# classes. A troca é feita por renomeação, então um jogo já aberto continua com a
# versão antiga até ser reiniciado.
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
mods_dir="${PRISM_MODS_DIR:-$HOME/.local/share/PrismLauncher/instances/IceAgeSurvival/minecraft/mods}"

jar="$(ls -t "$project_dir"/build/libs/iceagesurvival-*.jar 2>/dev/null | head -1)"
if [[ -z "$jar" ]]; then
    echo "Nenhum jar em build/libs; rode ./gradlew build antes." >&2
    exit 1
fi
if [[ ! -d "$mods_dir" ]]; then
    echo "Pasta de mods não encontrada: $mods_dir" >&2
    exit 1
fi

# Remove versões anteriores do mod (inclusive o atalho antigo) sem tocar nos outros mods.
find "$mods_dir" -maxdepth 1 -name 'iceagesurvival-*.jar' ! -name "$(basename "$jar")" -delete

tmp="$mods_dir/.$(basename "$jar").tmp"
cp "$jar" "$tmp"
mv -f "$tmp" "$mods_dir/$(basename "$jar")"
echo "Instalado: $mods_dir/$(basename "$jar")"
