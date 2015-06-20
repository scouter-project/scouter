
Host Monitoring Agent 
##########################################################
# Windows
##########################################################
python2.7
psutil 

c:\python27\python scouter.py

##########################################################
# Linux
##########################################################
python2.6, 2.7
### Prerequisite
  gcc, python-header
  * CentOS, RHEL, Fedora
     sudo yum install gcc
     sudo yum install python-devel
  * Debian and Ubuntu
     sudo apt-get install gcc
     sudo apt-get install python-dev
     
### Python Lib. to need
-  psutil (host agent)
//  wget https://bootstrap.pypa.io/ez_setup.py -O - | python   ( Setuptools : not last version)
//  https://pypi.python.org/packages/source/p/psutil/psutil-2.1.1.tar.gz (Recommended)
//  or
//  https://pypi.python.org/packages/source/p/psutil/psutil-1.2.1.tar.gz
//  tar -zxvf psutil-1.2.1.tar.gz
//  python setup.py install

  
### Executable file : Pyinstaller
    1. Install setuptools : sudo wget https://bitbucket.org/pypa/setuptools/raw/bootstrap/ez_setup.py -O - | python   ( Setuptools 최신버전 설치 금지)
    2. Download pyinstaller 
        wget --no-check-certificate https://pypi.python.org/packages/source/P/PyInstaller/PyInstaller-2.1.tar.gz
        tar -zxvf PyInstaller-2.1.tar.gz
        // python setup.py install
        python ../PyInstaller-2.1/pyinstaller.py scouter.host.py 
    3. Make executable file
        //pyinstaller --onefile hostperf.py (위의 방법으로 대체)