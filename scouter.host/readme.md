
#Scouter Host Agent 

## Windows
1. Prerequisite 
 - python2.7
 - psutil 

2. Execute
 -> c:\python27\python hostperf.py

## Linux
python2.6, 2.7
### Prerequisite
gcc, python-header
1. CentOS, RHEL, Fedora
   -> sudo yum install gcc
   -> sudo yum install python-devel
2. Debian and Ubuntu
   -> sudo apt-get install gcc
   -> sudo apt-get install python-dev
     
### Python Lib.(psutil)
Choose #1 or #2
1. install #1
 -> wget https://bootstrap.pypa.io/ez_setup.py -O - | python   ( Setuptools : not last version)
 -> https://pypi.python.org/packages/source/p/psutil/psutil-2.1.1.tar.gz (Recommended)
2. install #2
 -> https://pypi.python.org/packages/source/p/psutil/psutil-1.2.1.tar.gz
 -> tar -zxvf psutil-1.2.1.tar.gz
 -> python setup.py install

### Executable file : Pyinstaller
1. Install setuptools : 
   -> sudo wget https://bitbucket.org/pypa/setuptools/raw/bootstrap/ez_setup.py -O - | python 
2. Download pyinstaller 
   -> wget --no-check-certificate https://pypi.python.org/packages/source/P/PyInstaller/PyInstaller-2.1.tar.gz
   -> tar -zxvf PyInstaller-2.1.tar.gz
   -> python setup.py install
   -> python ../PyInstaller-2.1/pyinstaller.py hostperf.py 
3. Make executable file
   -> pyinstaller --onefile hostperf.py
