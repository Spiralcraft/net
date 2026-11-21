[[ -n "${_COMMON_LOADED:-}" ]] && return; _COMMON_LOADED=1
set -euo pipefail
shopt -s inherit_errexit
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

load_env() {
  local file=$1 line key val
  [[ -f $file ]] || return 0
  while IFS= read -r line || [[ -n $line ]]; do
    line=${line%$'\r'}                                   # strip CR
    [[ $line =~ ^[[:space:]]*(#|$) ]] && continue        # blank/comment
    if ! [[ $line =~ ^[[:space:]]*(export[[:space:]]+)?([A-Za-z_][A-Za-z0-9_]*)[[:space:]]*=[[:space:]]*(.*)$ ]]; then
      echo "load_env: bad line in $file: $line" >&2; return 1
    fi
    key=${BASH_REMATCH[2]}; val=${BASH_REMATCH[3]}
    if [[ $val =~ ^\"([^\"]*)\"[[:space:]]*(#.*)?$ || $val =~ ^\'([^\']*)\'[[:space:]]*(#.*)?$ ]]; then
      val=${BASH_REMATCH[1]}                             # quoted: take inside
    else
      val=${val%%[[:space:]]#*}                          # unquoted: drop " # comment"
      val=${val%"${val##*[![:space:]]}"}                 # trim trailing space
    fi
    [[ -v $key ]] && continue                            # respect existing env
    export "$key=$val"
  done < "$file"
}

load_env "$PROJECT_ROOT/.env"           # local overrides
load_env "$PROJECT_ROOT/env.defaults"   # committed defaults


abs_path() {
  local name=$1 val=${!1:-}
  [[ -z $val || $val == /* ]] && return 0
  printf -v "$name" '%s' "$PROJECT_ROOT/$val"
  export "$name"
}
for v in SDK; do abs_path "$v"; done

: "${SDK:?SDK must be set}"


: "${SCBUILD:=$SDK/build}"

load_env "$SDK/build.local/gitbash.env"

: "${ANTJAR:=$SDK/build/lib/ant-launcher.jar}"

if [ -z "${JAVA_HOME:-}" ]
then
  JAVA=java
else
  JAVA=$JAVA_HOME/bin/java
fi



export PROJECT_ROOT SDK SCBUILD ANTJAR JAVA