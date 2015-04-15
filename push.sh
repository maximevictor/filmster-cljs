FILMSTER_HOST=filmster # in ssh config
FILMSTER_PEM=~/.ssh/filmster_aws.pem
FILMSTER_PATH=/var/filmster

lein deps
lein uberjar

# rsync --rsh='ssh -i '$FILMSTER_PEM -vr --delete --exclude-from deploy/exclude.txt ./ $FILMSTER_USER@$FILMSTER_HOST:$FILMSTER_PATH
rsync -vr --delete --exclude-from deploy/exclude.txt ./ $FILMSTER_HOST:$FILMSTER_PATH
open $FILMSTER_HOST
