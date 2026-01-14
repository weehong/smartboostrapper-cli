#!/bin/bash
set -e

if [ -z "$GITHUB_USERNAME" ]; then
    echo "ERROR: GITHUB_USERNAME is not set"
    exit 1
fi

if [ -z "$GITHUB_TOKEN" ]; then
    echo "ERROR: GITHUB_TOKEN is not set"
    exit 1
fi

mkdir -p ~/.m2
cat <<EOF > ~/.m2/settings.xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>${GITHUB_USERNAME}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

chmod +x ./mvnw
./mvnw clean install -DskipTests -Dcheckstyle.skip=true
