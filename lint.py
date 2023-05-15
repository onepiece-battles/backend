#!/usr/bin/env python3

import argparse
import logging
import os
import re
import shutil
import subprocess
import sys
from importlib import util

#GOOGLE_JAVA_FORMAT = "google-java-format-1.17.0-all-deps.jar"
GOOGLE_JAVA_FORMAT_URL = ("https://github.com/google/google-java-format/releases/download/"
                          "v1.17.0/google-java-format-1.17.0-all-deps.jar")
GOOGLE_JAVA_FORMAT = "google-java-format-1.17.0-all-deps.jar"

def java_formatter(files, args):
    external_formatter = os.path.join(args.cache_dir, GOOGLE_JAVA_FORMAT)
    print('cache dir: {}'.format(args.cache_dir))
    if not os.path.isfile(external_formatter):
        print('Setting up google-java-format')
        os.makedirs(args.cache_dir, exist_ok=True)
        subprocess.run(['curl', '-L', GOOGLE_JAVA_FORMAT_URL, '-o', external_formatter], check=True)
        os.chmod(external_formatter, 0o755)
    if args.fix:
        opts = ['-i']
    else:
        opts = ['-n', '--set-exit-if-changed']
    cmd = ['java',
           '--add-exports', 'jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED',
           '--add-exports', 'jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED',
           '--add-exports', 'jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED',
           '--add-exports', 'jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED',
           '--add-exports', 'jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED', '-jar',
           external_formatter] + opts + files
    return subprocess.run(cmd, check=False).returncode == 0


FORMATERS = [
    (re.compile(r'.*\.java$'), java_formatter),
]


def parse_commandline(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument('-f', '--fix', action='store_true', help='Fix style errors')
    parser.add_argument('--cache-dir', default=os.path.expanduser('~\\.git-templates'), help='Cache DIR')
    parser.add_argument('files', nargs='*', default=[])
    return parser.parse_args(argv)


def main(argv):
    args = parse_commandline(argv)
    for pattern, formatter in FORMATERS:
        matched_files = [file for file in args.files if pattern.match(file)]
        if len(matched_files) > 0 and not formatter(matched_files, args):
            return 1
    return 0


if __name__ == '__main__':
    print('formatting code start:')
    sys.exit(main(sys.argv[1:]))
