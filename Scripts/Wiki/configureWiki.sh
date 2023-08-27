git clone git@github.com:RoanH/osu-wiki.git
cd osu-wiki
git remote add ppy git@github.com:ppy/osu-wiki.git
git fetch ppy
git switch wikisync
git reset --hard ppy/master