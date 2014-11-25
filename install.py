import os, os.path, sys
import zipfile, urllib2
import platform
import shutil, tempfile, json
import errno
import platform

from hashlib import md5  # pylint: disable-msg=E0611
from optparse import OptionParser

from applychanges import applychanges, apply_patch


base_dir = os.path.dirname(os.path.abspath(__file__))

mc_version = "1.8"
of_version = "1.8.0_HD_U_A5"
minecrift_version_num = "1.8.0"
minecrift_build = "PRE1"
of_file_extension = ".jar"
of_file_md5 = "57c48d49e442d3922bb43595a08f9c89"
mcp_version = "mcp910-pre1"
preferredarch = ''

try:
    WindowsError
except NameError:
    WindowsError = OSError

def osArch():
    if platform.machine().endswith('64'):
        return '64'
    else:
        return '32'

def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError as exc: # Python >2.5
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else: raise

#Helpers taken from forge mod loader, https://github.com/MinecraftForge/FML/blob/master/install/fml.py
def get_md5(file):
    if not os.path.isfile(file):
        return ""
    with open(file, 'rb') as fh:
        return md5(fh.read()).hexdigest()

def download_file(url, target, md5=None):
    name = os.path.basename(target)

    if not os.path.isfile(target):
        print 'Downloading: %s' % os.path.basename(target)
        try:
            with open(target,"wb") as tf:
                res = urllib2.urlopen(urllib2.Request( url, headers = {"User-Agent":"Mozilla/5.0"}))
                tf.write( res.read() )
            if not md5 == None:
                if not get_md5(target) == md5:
                    print 'Download of %s failed md5 check, deleting' % name
                    os.remove(target)
                    return False
            print 'Done'
        except Exception as e:
            print e
            print 'Download of %s failed, download it manually from \'%s\' to \'%s\'' % (target, url, target)
            return False
    else:
        print 'File Exists: %s' % os.path.basename(target)
    return True

def download_native(url, folder, name):
    if not os.path.exists(folder):
        os.makedirs(folder)

    target = os.path.join(folder, name)
    if not download_file(url, target):
        return False

    return True

def is_non_zero_file(fpath):  
    return True if os.path.isfile(fpath) and os.path.getsize(fpath) > 0 else False
    
def download_deps( mcp_dir ):
    if not os.path.exists(mcp_dir+"/runtime/commands.py "):
        download_file( "http://mcp.ocean-labs.de/files/archive/"+mcp_version+".zip", mcp_version+".zip" )
        try:
            os.mkdir( mcp_dir )
            mcp_zip = zipfile.ZipFile( mcp_version+".zip" )
            mcp_zip.extractall( mcp_dir )
            import stat
            astyle = os.path.join(mcp_dir,"runtime","bin","astyle-osx")
            st = os.stat( astyle )
            os.chmod(astyle, st.st_mode | stat.S_IEXEC)
        except:
            pass
            
    print("Patching mcp.cfg. Ignore \"FAILED\" hunks")
    apply_patch( mcp_dir, os.path.join("mcppatches", "mcp.cfg.patch"), os.path.join(mcp_dir,"conf"))
    print("Patching fffix.py. Ignore \"FAILED\" hunks")
    apply_patch( mcp_dir, os.path.join("mcppatches", "fffix.py.patch"), os.path.join(mcp_dir,"runtime","pylibs"))
    
    client_md5 = os.path.join("mcppatches","client.md5")
    target_client_md5 = os.path.join(mcp_dir,"temp","client.md5")
    if not os.path.exists(target_client_md5):
        mkdir_p( os.path.join(mcp_dir,"temp") )
        print 'Updating client.md5: copying %s to %s' % (client_md5, target_client_md5)
        shutil.copy(client_md5,target_client_md5)

    jars = os.path.join(mcp_dir,"jars")

    versions =  os.path.join(jars,"versions",mc_version)
    mkdir_p( versions )

    if sys.platform == 'darwin':
        native = "osx"
    elif sys.platform == "linux":
        native = "linux"
    elif sys.platform == "linux2":
        native = "linux"
    else:
        native = "windows"

    flat_lib_dir = os.path.join(base_dir,"lib",mc_version)
    flat_native_dir = os.path.join(base_dir,"lib",mc_version,"natives",native)
    mkdir_p( flat_lib_dir )
    mkdir_p( flat_native_dir )
        
    json_file = os.path.join(versions,mc_version+".json")
    source_json_file = os.path.join("installer",mc_version+".json")
    print 'Updating json: copying %s to %s' % (source_json_file, json_file)
    shutil.copy(source_json_file,json_file)
    

    optifine_dir = os.path.join(jars,"libraries","optifine","OptiFine",of_version )
    mkdir_p( optifine_dir )

    print 'Checking Optifine...'
    optifine_jar = "OptiFine-"+of_version+".jar"
    optifine_file = os.path.join( optifine_dir, optifine_jar )
    download_optifine = False
    optifine_md5 = ''
    if not is_non_zero_file( optifine_file ):
        download_optifine = True
    else:
        optifine_md5 = get_md5( optifine_file )
        print 'Optifine md5: %s' % optifine_md5
        if optifine_md5 != of_file_md5:
            download_optifine = True
            print 'Bad MD5!'
        else:
            print 'MD5 good!'
    
    if download_optifine: 
        optifine_url = "http://optifine.net/download.php?f=OptiFine_"+of_version+of_file_extension
        print 'Downloading Optifine...'
        if not download_file( optifine_url, optifine_file, of_file_md5 ):
            print 'FAILED to download Optifine!'
            sys.exit(1)
        else:
            shutil.copy(optifine_file,os.path.join(flat_lib_dir, os.path.basename(optifine_file)))

    json_obj = []
    with open(json_file,"rb") as f:
        #data=f.read()
        #print 'JSON File:\n%s' % data
        json_obj = json.load( f )
    try:
        newlibs = []
        for lib in json_obj['libraries']:
            libname = lib["name"]
            skip = False
            if "rules" in  lib:
                for rule in lib["rules"]:
                    if "action" in rule and rule["action"] == "allow" and "os" in rule:
                        skip = True
                        for entry in rule["os"]:
                            if "name" in entry:
                                if rule["os"]["name"] == native:
                                    skip = False

            if skip:
                print 'File: %s\nSkipping due to rules' % libname
                continue
                
            group,artifact,version = lib["name"].split(":")
            if "url" in lib:
                repo = lib["url"]
            else:
                repo = "https://libraries.minecraft.net/"

            if "natives" in lib:
                url = group.replace(".","/")+ "/"+artifact+"/"+version +"/"+artifact+"-"+version+"-"+lib["natives"][native]+".jar"
            else:
                url = group.replace(".","/")+ "/"+artifact+"/"+version +"/"+artifact+"-"+version+".jar"

            index = url.find('${arch}')
            if index > -1:
                # Get both 32 and 64 bit versions
                url32 = url.replace('${arch}', '32')
                file32 = os.path.join(jars,"libraries",url32.replace("/",os.sep))
                mkdir_p(os.path.dirname(file32))
                download_file( repo + url32, file32 )
                shutil.copy(file32,os.path.join(flat_lib_dir, os.path.basename(file32)))
                
                url64 = url.replace('${arch}', '64')
                file64 = os.path.join(jars,"libraries",url64.replace("/",os.sep))
                mkdir_p(os.path.dirname(file64))
                download_file(repo + url64, file64)
                shutil.copy(file64,os.path.join(flat_lib_dir, os.path.basename(file64)))                

                # Use preferred architecture to choose which natives to extract.
                if preferredarch is '32':
                    print '    Using preferred arch 32bit'
                    extractnatives( lib, jars, file32, flat_native_dir )
                else:
                    print '    Using preferred arch 64bit'
                    extractnatives( lib, jars, file64, flat_native_dir )                
               
            else:
                file = os.path.join(jars,"libraries",url.replace("/",os.sep))
                mkdir_p(os.path.dirname(file))
                download_file( repo + url, file )
                shutil.copy(file,os.path.join(flat_lib_dir, os.path.basename(file)))  
                extractnatives( lib, jars, file, flat_native_dir )
                
            newlibs.append( lib )
        json_obj['libraries'] = newlibs
        with open(json_file,"wb+") as f:
            json.dump( json_obj,f, indent=1 )
    except Exception as e:
        print 'ERROR: %s' % e
        raise

    repo = "https://s3.amazonaws.com/Minecraft.Download/"
    jar_file = os.path.join(versions,mc_version+".jar")
    jar_url = repo + "versions/"+mc_version+"/"+mc_version+".jar"
    download_file( jar_url, jar_file )
    shutil.copy(jar_file,os.path.join(flat_lib_dir, os.path.basename(jar_file))) 

def extractnatives( lib, jars, file, copydestdir ):
    if "natives" in lib:
        folder = os.path.join(jars,"versions",mc_version,mc_version+"-natives")
        mkdir_p(folder)
        zip = zipfile.ZipFile(file)
        #print 'Native extraction: folder: %s, file to unzip: %s' % (folder, file)
        for name in zip.namelist():
            if not name.startswith('META-INF') and not name.endswith('/'):
                out_file = os.path.join(folder, name)
                print '    Extracting native library %s' % name
                out = open(out_file, 'wb')
                out.write(zip.read(name))
                out.flush()
                out.close()
                shutil.copy(out_file,os.path.join(copydestdir, os.path.basename(out_file))) 

def zipmerge( target_file, source_file ):
    out_file, out_filename = tempfile.mkstemp()
    out = zipfile.ZipFile(out_filename,'a')
    try:
        target = zipfile.ZipFile( target_file, 'r')
    except Exception as e:
        print 'zipmerge: target not a zip-file: %s' % target_file
        raise

    try:        
        source = zipfile.ZipFile( source_file, 'r' )
    except Exception as e:
        print 'zipmerge: source not a zip-file: %s' % source_file
        raise
        
    #source supersedes target
    source_files = set( source.namelist() )
    target_files = set( target.namelist() ) - source_files

    for file in source_files:
        out.writestr( file, source.open( file ).read() )

    for file in target_files:
        out.writestr( file, target.open( file ).read() )

    source.close()
    target.close()
    out.close()
    os.remove( target_file )
    shutil.copy( out_filename, target_file )


def symlink(source, link_name):
    import os
    os_symlink = getattr(os, "symlink", None)
    if callable(os_symlink):
        try:
            os_symlink(source, link_name)
        except Exception:
            pass
    else:
        import ctypes
        csl = ctypes.windll.kernel32.CreateSymbolicLinkW
        csl.argtypes = (ctypes.c_wchar_p, ctypes.c_wchar_p, ctypes.c_uint32)
        csl.restype = ctypes.c_ubyte
        flags = 1 if os.path.isdir(source) else 0
        if csl(link_name, source, flags) == 0:
            raise ctypes.WinError()

def osArch():
    if platform.machine().endswith('64'):
        return '64'
    else:
        return '32'

def main(mcp_dir):
    print 'Using base dir: %s' % base_dir
    print 'Using mcp dir: %s (use -m <mcp-dir> to change)' % mcp_dir
    print 'Preferred architecture: %sbit - preferring %sbit native extraction (use -a 32 or -a 64 to change)' % (preferredarch, preferredarch)
    print("Downloading dependencies...")
    download_deps( mcp_dir )

    print("Applying Optifine...")
    optifine = os.path.join(mcp_dir,"jars","libraries","optifine","OptiFine",of_version,"OptiFine-"+of_version+".jar" )
    minecraft_jar = os.path.join( mcp_dir,"jars","versions",mc_version,mc_version+".jar")
    print ' Merging\n  %s\n into\n  %s' % (optifine, minecraft_jar)
    zipmerge( minecraft_jar, optifine )

    print("Decompiling...")
    src_dir = os.path.join(mcp_dir, "src","minecraft")
    if os.path.exists( src_dir ):
        shutil.rmtree( src_dir, True )
    sys.path.append(mcp_dir)
    os.chdir(mcp_dir)
    from runtime.decompile import decompile
    #         Conf  JAD    CSV    -r     -d     -a     -n     -p     -o     -l     -g     -c    -s     --rg   -w    json  --nocopy
    decompile(None, False, False, False, False, False, False, False, False, False, False, True, False, False, None, None, True  )

    os.chdir( base_dir )

    org_src_dir = os.path.join(mcp_dir, "src",".minecraft_orig")
    if os.path.exists( org_src_dir ):
        shutil.rmtree( org_src_dir, True )
    shutil.copytree( src_dir, org_src_dir )

    applychanges( mcp_dir )


if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option('-m', '--mcp-dir', action='store', dest='mcp_dir', help='Path to MCP to use', default=None)
    parser.add_option('-a', '--architecture', action='store', dest='arch', help='Architecture to use (\'32\' or \'64\'); prefer 32 or 64bit dlls', default=None)
    options, _ = parser.parse_args()

    if not options.arch is None:
        if options.arch is '32':
            preferredarch = '32'
        elif options.arch is '64':
            preferredarch = '64'
            
    if preferredarch is '':
        preferredarch = osArch()
    
    if not options.mcp_dir is None:
        main(os.path.abspath(options.mcp_dir))
    elif os.path.isfile(os.path.join('..', 'runtime', 'commands.py')):
        main(os.path.abspath('..'))
    else:
        main(os.path.abspath(mcp_version))
