DIR="$(pwd)"/.cache
mkdir -p "$DIR"

FILE=$DIR/linux-amd64/helm

if test -f "$FILE"; then
  echo "$FILE exist"
else
  echo "$FILE does not exist"
  curl -fsSL -o "$DIR"/helm.tar.gz https://get.helm.sh/helm-v3.2.3-linux-amd64.tar.gz
  cd "$DIR" && tar -xzvf helm.tar.gz && rm -rf helm.tar.gz && cd ..
fi

# shellcheck disable=SC2139
alias helm="$DIR/linux-amd64/helm"

printf '\n'
helm version
printf '\n'

DEPLOYMENT="rtms"

option="${1}"
case ${option} in
   -s)
        kubectl create secret docker-registry github-cloud-rtms \
        --docker-server=docker.pkg.github.com \
        --docker-username=BhuwanUpadhyay \
        --docker-password="$GITHUB_TOKEN" \
        --docker-email=bot.bhuwan@gmail.com
      ;;
   --deploy)
        helm upgrade \
        --install -f rtms/env/development/values.yaml \
        $DEPLOYMENT rtms --force
      ;;
   --update-deps)
        helm dependency update rtms
      ;;
   --add-repos)
        helm repo add bitnami https://charts.bitnami.com/bitnami
        helm repo add chartmuseum http://localhost:18080
      ;;
   --delete)
      helm delete $DEPLOYMENT
      ;;
   *)
      echo "`basename ${0}`:usage: [--deploy] | [--upgrade-deps] | [--add-repos] | [--delete]"
      exit 1 # Command to come out of the program with status 1
      ;;
esac