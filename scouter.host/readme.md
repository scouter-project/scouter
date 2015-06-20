
#Scouter Host Agent 

## Windows
- prerequsite
python2.7
psutil 

- execute
c:\python27\python hostperf.py

## Linux
python2.6, 2.7
### Prerequisite
  gcc, python-header
  * CentOS, RHEL, Fedora
     sudo yum install gcc
     sudo yum install python-devel
  * Debian and Ubuntu
     sudo apt-get install gcc
     sudo apt-get install python-dev
     
### Python Lib.(psutil)
you can choose #1 or #2
- install #1
  wget https://bootstrap.pypa.io/ez_setup.py -O - | python   ( Setuptools : not last version)
  https://pypi.python.org/packages/source/p/psutil/psutil-2.1.1.tar.gz (Recommended)
- install #2
  https://pypi.python.org/packages/source/p/psutil/psutil-1.2.1.tar.gz
  tar -zxvf psutil-1.2.1.tar.gz
  python setup.py install

  
### Executable file : Pyinstaller
    1. Install setuptools : 
       sudo wget https://bitbucket.org/pypa/setuptools/raw/bootstrap/ez_setup.py -O - | python   
       ( Setuptools 최신버전 설치 금지)
    2. Download pyinstaller 
        wget --no-check-certificate https://pypi.python.org/packages/source/P/PyInstaller/PyInstaller-2.1.tar.gz
        tar -zxvf PyInstaller-2.1.tar.gz
        python setup.py install
        python ../PyInstaller-2.1/pyinstaller.py scouter.host.py 
    3. Make executable file
        pyinstaller --onefile hostperf.py
