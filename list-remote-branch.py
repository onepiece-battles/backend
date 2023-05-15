#!/usr/bin/env python3

"""
Used to find out owner of remote branch, owner is determined by the last commit.


Note: GitPython is required.
Install it by: pip install GitPython

"""
from git import Repo
from git import Git
import re
import time
import os
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('-l', '--ldap', help='Highlight record of target ldap')
parser.add_argument('-n', '--name', nargs='+', help='Highlight record of target name')
args = parser.parse_args()
full_name = None
input_ldap = None
if args.name is not None:
    full_name = " ".join([_ for _ in args.name])
if args.ldap is not None:
    input_ldap =  args.ldap

class Style:
    BOLD = '\033[1m'
    END = '\033[0m'
    YELLOW = '\033[93m'

def print_with_style(style:str, branch_name:str, author_name:str, ldap:str):
    if input_ldap is not None and input_ldap.lower().lstrip() in ldap:
        print("{}{:<60}{:<20}{}{}".format(style,branch_name, author_name, ldap, Style.END))
    elif full_name is not None and full_name.lower().lstrip() in author_name.lower():
        print("{}{:<60}{:<20}{}{}".format(style,branch_name, author_name, ldap, Style.END))
    else:
        print("{:<60}{:<20}{}".format(branch_name, author_name, ldap))

def fetch_remote_branch():
    print('start fetching remote branch')
    base_dir = os.getcwd()
    r = Git(base_dir)
    local_repo = Repo(path=base_dir)

    remotes = local_repo.remotes

    branch_to_author_less_than_one_week = {}
    branch_to_author_less_than_one_month = {}
    branch_to_author_more_than_one_month = {}
    author_to_ldap = {}
    for branch in remotes[0].fetch():
        one_month_line = 30 * 24 * 3600
        one_week_line = 7 * 24 * 3600
        current_time = time.time()
        hash_for_commit = str(branch.commit)
        full = ['git', 'cat-file', '-p', hash_for_commit]
        result = r.execute(full)
        # regex result includes 3 groups:
        # group1->author info
        # group2->ldap info
        # group3->time info
        reg_result = re.search(r'author(.*)<(.*@.*)>\s(\d{10})', str(result))
        if reg_result is None:
            continue
        author = str(reg_result.group(1))
        ldap = reg_result.group(2)
        commit_time = reg_result.group(3)
        branch_name = str(branch.name)[7:]
        author_to_ldap[author] = ldap
        if (current_time - int(commit_time)) > one_month_line:
            branch_to_author_more_than_one_month[branch_name] = author
        else:
            branch_to_author_less_than_one_month[branch_name] = author
            if (current_time - int(commit_time)) < one_week_line:
                branch_to_author_less_than_one_week[branch_name] = author
    print("{}{}More than one month:{}".format(Style.YELLOW,Style.BOLD,Style.END))
    for key, value in branch_to_author_more_than_one_month.items():
        ldap = author_to_ldap.get(value)
        print_with_style(Style.YELLOW,key, value,ldap)
    print("{}{}Less than one month:{}".format(Style.YELLOW,Style.BOLD,Style.END))
    for key, value in branch_to_author_less_than_one_month.items():
        ldap = author_to_ldap.get(value)
        print_with_style(Style.YELLOW,key, value,ldap)
    print("{}{}Less than one week:{}".format(Style.YELLOW,Style.BOLD,Style.END))
    for key, value in branch_to_author_less_than_one_week.items():
        ldap = author_to_ldap.get(value)
        print_with_style(Style.YELLOW,key, value,ldap)

if __name__ == '__main__':
    fetch_remote_branch()
