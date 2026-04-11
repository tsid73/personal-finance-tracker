find . -type f -name '*:Zone.Identifier' -delete;
git status; git add .; git commit -m "$1";  git push;