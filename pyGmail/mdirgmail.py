#!/usr/bin/python
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Library General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
import os, sys, time, string, ConfigParser, libgmail

def mySyntax():
		"""
		Print out the syntax for mdirgmail.py
		"""
		result = "mdirgmail.py v0.2.1\nUSAGE 0: mdirgmail.py <username> <password> <label> <base-maildir> [--unread-only] [--written <filename>]"
		result += "\nUSAGE 1: mdirgmail.py --help"
		result += "\nUSAGE 2: mdirgmail.py [--conf <filename>] [--written <filename>]"
		result += "\nusername\tYour gmail account username."
		result += "\npassword\tYour gmail account password."
		result += "\nlabel\t\tThe label whose messages you want to download."
		result += "\nbase-maildir\tBase Maildir directory(make sure it ends with a \"/\"(or \"\\\" for windows users)."
		result += "\n--conf\t\tSpecify a filename to read configuration options from."
		result += "\n--written\tSpecify a filename to read and write info about messages." 
		result += "\n--unread-only\tIf you want to download unread mails only."
		result += "\n--help\t\tPrint this help message. This should be passed individually."
		return result

def checkIfExists(filename):
	if os.path.exists(filename) == False:
		sys.stderr.write("The specified file/Directory: %s does not exist. Please check the path.\n%s\n" % (filename,mySyntax()))
		sys.exit(1)

class AnalyseOpts:
	def _writtenParsed(self, args, index):
		try:
			return args[index]
		except IndexError:
			sys.stderr.write("ERROR: You passed the --written option but did not specify a file.\n%s\n" % mySyntax())
			sys.exit(2)
			
	def __init__(self, args):
		self.writtenFile = ".mdirgmailwritten"
		self.useDB = False
		self.unreadOnly = False
		#read the various required parameters.
		try:	
			self.username = args[1]
		except IndexError:
			sys.stderr.write("ERROR: No username provided.\n%s\n" % mySyntax())
			sys.exit(2)
			
		try:
			self.password = args[2]
		except IndexError:
			sys.stderr.write("ERROR: No password provided.\n%s\n" % mySyntax())
			sys.exit(2)
		
		try:
			self.myLabel = args[3].split(",")
		except IndexError:
			sys.stderr.write("ERROR: You did not specify a label whose messages you want to download.\n%s\n" % mySyntax())
			sys.exit(2)
			
		try:
			self.myMdir = args[4]
			checkIfExists(self.myMdir)
		except IndexError:
			sys.stderr.write("ERROR: You did not specify a base maildir directory\n%s\n" % mySyntax())
			sys.exit(2)
		
		for i in range(5,len(args)):
			try:
				if args[i] == "--unread-only":
					self.unreadOnly = True
				elif args[i] == "--written":
					self.writtenFile = self._writtenParsed(args, i+1)
					checkIfExists(self.writtenFile)
					break
				else:
					sys.stderr.write("ERROR: Unrecognised options: " + args[i] + ".\n%s\n" % mySyntax())
					sys.exit(2)
			except IndexError:
				pass

class ReadOpts:
	def __init__(self, confile):
		self.confFile = confile
		self.config = ConfigParser.ConfigParser()
		self.config.read(self.confFile)
		self.username = self.config.get("DEFAULT","username")
		self.password = self.config.get("DEFAULT","password")
		self.myMdir = self.config.get("DEFAULT","baseMdir")
		if self.username == "" or self.password == "" or self.myMdir == "":
			sys.stderr.write("ERROR: The configuration file did not specify one or more of username, password or baseMdir.\n%s\n"% mySyntax())
			sys.exit(2)
		checkIfExists(self.myMdir)
		self.myLabel = self.config.get("DEFAULT","label").split(",")
		if self.myLabel == "":
			self.myLabel = ["inbox"]
		try:
			self.unreadOnly = self.config.getboolean("DEFAULT","unread-only")
		except ValueError:
			self.unreadOnly = False
		try:
			self.useDB = self.config.getboolean("DEFAULT","use-mysql")
		except ValueError:
			self.useDB = False
		if self.useDB:
			self.dbUser = self.config.get("DEFAULT","mysql-username")
			self.dbPass = self.config.get("DEFAULT","mysql-password")
			self.dbName = self.config.get("DEFAULT","mysql-database")
			self.dbHost = self.config.get("DEFAULT","mysql-host")

def whichOpts():
	if len(sys.argv) <= 1:
		return -2
	elif len(sys.argv) == 2 and sys.argv[1] == "--help":
		return -1
	elif sys.argv[1] == "--conf" or sys.argv[1] == "--written":
		return 0
	return 1

def isMdir(mdir):
	dirs = os.listdir(mdir)
	if "new" not in dirs:
		return False
	if "tmp" not in dirs:
		return False
	if "cur" not in dirs:
		return False
	return True

def getConfWritten(args):
	mylist = ['.mdirgmailrc','.mdirgmailwritten']
	for i in range(1,len(args),2):
		if args[i] == "--conf":
			try:
				mylist[0] = args[i + 1]
				checkIfExists(mylist[0])
			except IndexError:
				sys.stderr.write("ERROR: You did not specify a configuration file after passing the --conf option.\n%s\n" %mySyntax())
				sys.exit(2)
		elif args[i] == "--written":
			try:
				mylist[1]= args[i + 1]
				checkIfExists(mylist[1])
			except IndexError:
				sys.stderr.write("ERROR: You did not specify a file after passing the --written option.\n%s\n" %mySyntax())
				sys.exit(2)
		else:
			sys.stderr.write("You passed an unrecognised option: " + args[i] + "\n%s\n" % mySyntax())
			sys.exit(2)
	return mylist

if __name__ == "__main__":
	global writtenfile
	if whichOpts() == -1:
		print "%s" % mySyntax()
		sys.exit(0)
	elif whichOpts() == 0 or whichOpts() == -2:
		list = getConfWritten(sys.argv)
		print "Using configuration file: %s" % (list[0])
		myOpts = ReadOpts(list[0])
		if myOpts.useDB == False:
			writtenfile = list[1]
			print "\nUsing written file: %s" % (list[1])
	else:
		myOpts = AnalyseOpts(sys.argv)
		writtenfile = myOpts.writtenFile
		print "Using written file: %s" % writtenfile
	
	if myOpts.useDB:
		import MySQLdb, _mysql_exceptions
		myDBConn = MySQLdb.connect(host=myOpts.dbHost,user=myOpts.dbUser,passwd=myOpts.dbPass,db=myOpts.dbName)
		myDBCursor = myDBConn.cursor()
	else:
		myWritten = ConfigParser.ConfigParser()
		myWritten.read(writtenfile)
	
	if isMdir(myOpts.myMdir) == False:
		sys.stderr.write("ERROR: The base maildir directory is not a maildir directory. Make sure it has three subdirectories: new, cur and tmp.\n")
		sys.exit(1)

	if(myOpts.unreadOnly):
		print "Fetching unread messages only."

	myGmailContact = libgmail.GmailAccount(myOpts.username, myOpts.password)
	print "Logging in...."
	
	try:
		myGmailContact.login()
		print "Logged in"
	except libgmail.GmailLoginFailure:
		sys.stderr.write("ERROR: Error in logging in. Wrong username/password??\n%s\n" % mySyntax())
		sys.exit(2)
	
	#Function to download and save all mails according to passed filename and data.
	def populateAll(tmpname, finalname, data):
		"""
		This function does the actual work of creating and writing to a file.
		"""
		myFile = open(tmpname, "w")
		myFile.write(data)
		myFile.close()
		os.rename(tmpname, finalname)
	
	#Function to get the no. of messages written for a particular conversation.
	def getNum(id,label):
		"""
		This function queries the written file to give a no. of messages already written for a particular thread.
		"""
		if myOpts.useDB:
			try:
				myDBCursor.execute("SELECT length FROM %s WHERE `id` = \"%s\"" %(str.lower(label),id))
				result = myDBCursor.fetchall()
				try:
					return result[0][0]
				except IndexError:
					return 0
			except _mysql_exceptions.OperationalError:
				return 0
			except _mysql_exceptions.ProgrammingError:
				return 0
		else:
			try:
				return myWritten.getint(label,id)
			except ConfigParser.NoOptionError:
				return 0
			except ConfigParser.NoSectionError:
				return 0
	
	def setNum(label, id, length):
		if myOpts.useDB:
			try:
				myDBCursor.execute("INSERT INTO %s(id,length) VALUES(\"%s\",%i)"%(str.lower(label),id,int(length)))
			except _mysql_exceptions.ProgrammingError:
				print("Creating tabel: %s"% label)
				myDBCursor.execute("CREATE TABLE `%s` (`id` VARCHAR(20) NOT NULL,`length` INT DEFAULT '0' NOT NULL,UNIQUE (`id`))"% str.lower(label))
				myDBCursor.execute("INSERT INTO %s(id,length) VALUES(\"%s\",%i)"%(str.lower(label),id,int(length)))
			except _mysql_exceptions.IntegrityError:
				myDBCursor.execute("UPDATE %s SET `length` = %i WHERE `id` = \"%s\"" %(str.lower(label),int(length),id))
		else:
			try:
				myWritten.set(label,id,length)
			except ConfigParser.NoSectionError:
				myWritten.add_section(label)
				myWritten.set(label,id,length)
			
	allLabels = libgmail.STANDARD_FOLDERS + myGmailContact.getLabelNames()
	for label in myOpts.myLabel:
		if label not in allLabels:
			sys.stderr.write("The specified label: " + myOpts.myLabel + " does not exist.\n")
			sys.exit(1)
	
		#Only if we have a request for inbox do we not create directories.
		if string.lower(label) != "inbox":
			try:
				os.makedirs(os.path.join(myOpts.myMdir,label,"new"))
				os.makedirs(os.path.join(myOpts.myMdir,label,"cur"))
				os.makedirs(os.path.join(myOpts.myMdir,label,"tmp"))
			except OSError:
				pass
	
		counter = 0
		flag = False	
		print "Fetching label: %s" % label
		result = myGmailContact.getMessagesByLabel(label, True)
		
		if len(result):
			for thread in result:
				for msg in thread:
					if counter == 0:
						numWritten = getNum(msg.id,label)
						if myOpts.unreadOnly == True:
							if len(thread) <= numWritten:
								flag = True
								break
						print str(numWritten) + " " + msg.subject
						if numWritten != len(thread):
							setNum(label,msg.id,len(thread))
					if counter >= numWritten:
						print "Writing message: " + msg.id
						if string.lower(label) == "inbox":
							populateAll(os.path.join(myOpts.myMdir,"tmp",str(int(time.time())) + "." + msg.id),os.path.join(myOpts.myMdir,"new",str(int(time.time())) + "." + msg.id),msg.source.replace("\r","").lstrip())
						else:
							populateAll(os.path.join(myOpts.myMdir,label,"tmp",str(int(time.time())) + "." + msg.id),os.path.join(myOpts.myMdir,label,"new",str(int(time.time())) + "." + msg.id), msg.source.replace("\r","").lstrip())
					counter = counter + 1
				if flag == True:
					break
				counter = 0
		print "%d threads/conversations" % len(result)
	if not myOpts.useDB:
		myWritten.write(open(writtenfile,"w"))
	print "Done\n"