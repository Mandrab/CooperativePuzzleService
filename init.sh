#!/bin/sh

if [ ! -e "trash" ]; then
  echo "Creating thrash folder..."
  mkdir "trash"
fi
python -m http.server --directory ./ 9000