import os, os.path, sys, json, datetime, StringIO
import shutil, tempfile,zipfile, fnmatch
from optparse import OptionParser
import subprocess, shlex

from install import download_deps, download_native, download_file, mkdir_p, mc_version, mcp_version, minecrift_version_num, minecrift_build

try:
    WindowsError
except NameError:
    WindowsError = OSError

base_dir = os.path.dirname(os.path.abspath(__file__))

def cmdsplit(args):
    if os.sep == '\\':
        args = args.replace('\\', '\\\\')
    return shlex.split(args)

def zipmerge( target_file, source_file ):
    out_file, out_filename = tempfile.mkstemp()
    out = zipfile.ZipFile(out_filename,'a')
    target = zipfile.ZipFile( target_file, 'r')
    source = zipfile.ZipFile( source_file, 'r' )

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

def process_json( addon, version ):
    json_id = "minecrift-"+version+addon
    lib_id = "com.mtbs3d:minecrift:"+version
    time = datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S-05:00")
    with  open(os.path.join("installer",mc_version+addon+".json"),"rb") as f:
        json_obj = json.load(f)
        json_obj["id"] = json_id
        json_obj["time"] = time
        json_obj["releaseTime"] = time
        json_obj["libraries"].insert(0,{"name":lib_id}) #Insert at beginning
        json_obj["libraries"].append({"name":"net.minecraft:Minecraft:"+mc_version}) #Insert at end
        return json.dumps( json_obj, indent=1 )

def create_install(mcp_dir):
    print("Creating Installer...")
    reobf = os.path.join(mcp_dir,'reobf','minecraft')
    
    in_mem_zip = StringIO.StringIO()
    with zipfile.ZipFile( in_mem_zip,'w', zipfile.ZIP_DEFLATED) as zipout:
        for abs_path, _, filelist in os.walk(reobf, followlinks=True):
            arc_path = os.path.relpath( abs_path, reobf ).replace('\\','/').replace('.','')+'/'
            for cur_file in fnmatch.filter(filelist, '*.class'):
                if cur_file=='blk.class': #skip SoundManager
                    continue
                in_file= os.path.join(abs_path,cur_file) 
                arcname =  arc_path + cur_file
                zipout.write(in_file, arcname)

    os.chdir( base_dir )

    
    in_mem_zip.seek(0)
    if os.getenv("RELEASE_VERSION"):
        version = os.getenv("RELEASE_VERSION")
    elif os.getenv("BUILD_NUMBER"):
        version = "b"+os.getenv("BUILD_NUMBER")
    else:
        version = minecrift_build

    version = minecrift_version_num+"-"+version
	
    artifact_id = "minecrift-"+version
    installer_id = artifact_id+"-installer"
    installer = os.path.join( installer_id+".jar" ) 
    shutil.copy( os.path.join("installer","installer.jar"), installer )
    with zipfile.ZipFile( installer,'a', zipfile.ZIP_DEFLATED) as install_out: #append to installer.jar
        install_out.writestr( "version.json", process_json("", version))
        install_out.writestr( "version-forge.json", process_json("-forge", version))
        install_out.writestr( "version-nohydra.json", process_json("-nohydra", version))
        install_out.writestr( "version-forge-nohydra.json", process_json("-forge-nohydra", version))
        install_out.writestr( "version.jar", in_mem_zip.read() )
        install_out.writestr( "version", artifact_id+":"+version )

    print("Creating Installer exe...")
    with open( os.path.join("installer","launch4j.xml"),"r" ) as inlaunch:
        with open( "launch4j.xml", "w" ) as outlaunch:
            outlaunch.write( inlaunch.read().replace("installer",installer_id))
    subprocess.Popen( 
        cmdsplit("java -jar \"%s\" \"%s\""% (
                os.path.join( base_dir,"installer","launch4j","launch4j.jar"),
                os.path.join( base_dir, "launch4j.xml"))), 
            cwd=os.path.join(base_dir,"installer","launch4j"),
            bufsize=-1).communicate()
    os.unlink( "launch4j.xml" )

def main(mcp_dir):
    print 'Using mcp dir: %s' % mcp_dir
    print 'Using base dir: %s' % base_dir
    
    print("Refreshing dependencies...")
    download_deps( mcp_dir )
    
    sys.path.append(mcp_dir)
    os.chdir(mcp_dir)

    reobf = os.path.join(mcp_dir,'reobf','minecraft')
    try:
        pass
        shutil.rmtree(reobf)
    except OSError:
        pass

    print("Recompiling...")
    from runtime.mcp import recompile_side, reobfuscate_side
    from runtime.commands import Commands, CLIENT
    commands = Commands(None, verify=True)
    recompile_side( commands, CLIENT)

    print("Reobfuscating...")
    commands.creatergcfg(reobf=True, keep_lvt=True, keep_generics=True, srg_names=False)
    reobfuscate_side( commands, CLIENT )
    create_install( mcp_dir )
    
if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option('-m', '--mcp-dir', action='store', dest='mcp_dir', help='Path to MCP to use', default=None)
    options, _ = parser.parse_args()

    if not options.mcp_dir is None:
        main(os.path.abspath(options.mcp_dir))
    elif os.path.isfile(os.path.join('..', 'runtime', 'commands.py')):
        main(os.path.abspath('..'))
    else:
        main(os.path.abspath(mcp_version))	
