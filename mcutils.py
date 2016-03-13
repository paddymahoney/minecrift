import os
import sys
import stat
import shutil
import errno
import platform
import shutil
import time
import filecmp
from shutil import move
from tempfile import mkstemp
from os import remove, close
from filecmp import dircmp
from minecriftversion import mc_version, of_file_name, of_json_name, minecrift_version_num, \
    minecrift_build, of_file_extension, of_file_md5, mcp_version, mc_file_md5, \
    mcp_download_url, mcp_uses_generics, forge_version, mcp_mappings

def doForgeFileDiffs(mcp_dir, mcp_dir_clean, minecrift_forge_src_dir, mcp_patch_dir):

    # start from scratch
    if os.path.exists(minecrift_forge_src_dir):
        reallyrmtree(minecrift_forge_src_dir)

    # get clean minecraft files
    orig_clean_mc_src_dir = os.path.join(mcp_dir_clean, 'src', 'minecraft')
    if not os.path.exists(orig_clean_mc_src_dir):
        raise IOError("Clean MCP decompile directory does not exist! Run install with -i (includeForge) option.")
    clean_mc_src_dir = os.path.join(minecrift_forge_src_dir, 'minecraft')
    copy_and_overwrite(orig_clean_mc_src_dir, clean_mc_src_dir)

    # optifine files - create diff of new files between original MC decompile, versus MC+Optifine (fixed)
    mc_opt_src_dir = os.path.join(mcp_dir, 'src', '.minecraft_orig')
    optifineSrc = get_dir_diff_rel(clean_mc_src_dir, mc_opt_src_dir, mc_opt_src_dir)
    for file in optifineSrc:
        copyFile(os.path.join(mc_opt_src_dir, file), os.path.join(minecrift_forge_src_dir, 'optifine', file))
        if os.path.exists(os.path.join(clean_mc_src_dir, file)):
            os.remove(os.path.join(clean_mc_src_dir, file))

    # minecrift files - create diff of new files between MC+opt (fixed) decompile, versus MC+Optifine+Minecrift
    mc_opt_mc_src_dir = os.path.join(mcp_dir, 'src', 'minecraft')
    minecriftSrc = get_dir_diff_rel(mc_opt_src_dir, mc_opt_mc_src_dir, mc_opt_mc_src_dir)
    for file in minecriftSrc:
        copyFile(os.path.join(mc_opt_mc_src_dir, file), os.path.join(minecrift_forge_src_dir, 'minecrift', file))
        if os.path.exists(os.path.join(minecrift_forge_src_dir, 'optifine', file)):
            os.remove(os.path.join(minecrift_forge_src_dir, 'optifine', file))
        if os.path.exists(os.path.join(clean_mc_src_dir, file)):
            os.remove(os.path.join(clean_mc_src_dir, file))

    # remove empty folders
    removeEmptyFolders(minecrift_forge_src_dir)

    # copy Start.java
    copyFile(os.path.join(mcp_patch_dir, 'Start.java'), os.path.join(minecrift_forge_src_dir, 'minecrift', 'Start.java'))


def removeEmptyFolders(path):
    if not os.path.isdir(path):
        return

    # remove empty subfolders
    files = os.listdir(path)
    if len(files):
        for f in files:
            fullpath = os.path.join(path, f)
            if os.path.isdir(fullpath):
                removeEmptyFolders(fullpath)

    # if folder empty, delete it
    files = os.listdir(path)
    if len(files) == 0:
        os.rmdir(path)


def copyFile(src, dest):
    dest_dir = os.path.dirname(dest)
    if not os.path.exists(dest_dir):
        os.makedirs(dest_dir)
    shutil.copy2(src, dest)


def getLibDir(base_dir):
    return os.path.join(base_dir,"lib")


def getLibDirVanilla(base_dir):
    return os.path.join(base_dir,"lib",mc_version)


def getLibDirForge(base_dir):
    return os.path.join(base_dir,"lib",mc_version+"-forge")


def copy_and_overwrite(from_path, to_path):
    if os.path.exists(to_path):
        shutil.rmtree(to_path)
    shutil.copytree(from_path, to_path)


def get_dir_diff_rel(dir1, dir2, diff_root):

    print ''
    abs_diff = []
    rel_diff = []
    dcmp = deepdircmp(dir1, dir2)
    getDiff(dcmp, abs_diff)
    abs_diff.sort()
    for abs_file in abs_diff:
        rel_file = os.path.relpath(abs_file, diff_root)
        rel_diff.append(rel_file)
        #print rel_file

    return rel_diff


def getDiff(dcmp, diff):

    #dcmp.report_full_closure()
    #print 'L: ' + dcmp.left
    #print 'R: ' + dcmp.right
    new_dirs = []

    for name in dcmp.diff_files:
        file_or_dir = os.path.join(dcmp.right, name)
        if os.path.isfile(file_or_dir):
            diff.append(file_or_dir)

    for name in dcmp.right_only:
        file_or_dir = os.path.join(dcmp.right, name)
        if os.path.isfile(file_or_dir):
            diff.append(file_or_dir)
        else:
            new_dirs.append(file_or_dir)

    for new_dir in new_dirs:
        for root, subdirs, files in os.walk(new_dir):
            for file in files:
                filename = os.path.join(root, file)
                diff.append(filename)

    # subdirs are common_dirs in both dirs
    for sub_dcmp in dcmp.subdirs.values():
        getDiff(sub_dcmp, diff)


def reallyrmtree(path):
    if not sys.platform.startswith('win'):
        if os.path.exists(path):
            shutil.rmtree(path)
    else:
        i = 0
        try:
            while os.stat(path) and i < 20:
                shutil.rmtree(path, onerror=rmtree_onerror)
                i += 1
        except OSError:
            pass

        # raise OSError if the path still exists even after trying really hard
        try:
            os.stat(path)
        except OSError:
            pass
        else:
            raise OSError(errno.EPERM, "Failed to remove: '" + path + "'", path)


def rmtree_onerror(func, path, _):
    if not os.access(path, os.W_OK):
        os.chmod(path, stat.S_IWUSR)
    time.sleep(0.5)
    try:
        func(path)
    except OSError:
        pass


def replacelineinfile(file_path, pattern, subst, firstmatchonly=False):
    #Create temp file
    fh, abs_path = mkstemp()
    new_file = open(abs_path,'wb')
    old_file = open(file_path,'rb')
    hit = False
    for line in old_file:
        if pattern in line and not (firstmatchonly == True and hit == True):
            new_file.write(subst)
            hit = True
        else:
            new_file.write(line)
    #close temp file
    new_file.close()
    close(fh)
    old_file.close()
    #Remove original file
    remove(file_path)
    #Move new file
    move(abs_path, file_path)


class deepdircmp(dircmp):
    """
    Compare the content of dir1 and dir2. In contrast with filecmp.dircmp, this
    subclass compares the content of files with the same path.
    """
    def phase3(self):
        """
        Find out differences between common files.
        Ensure we are using content comparison with shallow=False.
        """
        fcomp = filecmp.cmpfiles(self.left, self.right, self.common_files,
                                 shallow=False)
        self.same_files, self.diff_files, self.funny_files = fcomp