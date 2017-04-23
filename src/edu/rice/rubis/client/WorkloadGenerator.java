package edu.rice.rubis.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.GregorianCalendar;
import java.util.Scanner;

import edu.rice.rubis.beans.TimeManagement;

/**
 * RUBiS client emulator. This class plays random user sessions emulating a Web
 * browser.
 *
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and
 *         <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */
public class WorkloadGenerator {
	private RUBiSProperties rubis = null; // access to rubis.properties file
	private URLGenerator urlGen = null; // URL generator corresponding to the
										// version to be used (PHP, EJB or
										// Servlets)
	private static float slowdownFactor = 0;
	private static boolean endOfSimulation = false;

	/**
	 * Creates a new <code>WorkloadGenerator</code> instance. The program is
	 * stopped on any error reading the configuration files.
	 */
	public WorkloadGenerator(String propertiesFileName) {
		// Initialization, check that all files are ok
		rubis = new RUBiSProperties(propertiesFileName);
		urlGen = rubis.checkPropertiesFileAndGetURLGenerator();
		if (urlGen == null)
			Runtime.getRuntime().exit(1);
		// Check that the transition table is ok and print it

		Stats stats = new Stats(rubis.getNbOfRows());
		TransitionTable transitionTable = new TransitionTable(rubis.getNbOfColumns(), rubis.getNbOfRows(), stats,
				rubis.useTPCWThinkTime(), rubis.getTransitionTable());

		if (!transitionTable.ReadExcelTextFile(rubis.getTransitionTable()))
			Runtime.getRuntime().exit(1);
		else
			transitionTable.displayMatrix();
	}

	/**
	 * Updates the slowdown factor.
	 *
	 * @param newValue
	 *            new slowdown value
	 */
	private synchronized void setSlowDownFactor(float newValue) {
		slowdownFactor = newValue;
	}

	/**
	 * Get the slowdown factor corresponding to current ramp (up, session or
	 * down).
	 *
	 * @return slowdown factor of current ramp
	 */
	public static synchronized float getSlowDownFactor() {
		return slowdownFactor;
	}

	/**
	 * Set the end of the current simulation
	 */
	private synchronized void setEndOfSimulation() {
		endOfSimulation = true;
	}

	/**
	 * True if end of simulation has been reached.
	 * 
	 * @return true if end of simulation
	 */
	public static synchronized boolean isEndOfSimulation() {
		return endOfSimulation;
	}

	/**
	 * Start the monitoring program specified in rubis.properties on a remote
	 * node and redirect the output in a file local to this node (we are more
	 * happy if it is on a NFS volume)
	 *
	 * @param node
	 *            node to launch monitoring program on
	 * @param outputFileName
	 *            full path and name of file to redirect output into
	 * @return the <code>Process</code> object created
	 */
	private Process startMonitoringProgram(String node, String outputFileName) {
		/*
		 * int fullTimeInSec =
		 * (rubis.getUpRampTime()+rubis.getSessionTime()+rubis.getDownRampTime()
		 * )/1000 + 5; // Give 5 seconds extra for init try { String[] cmd = new
		 * String[3]; cmd[0] = rubis.getMonitoringRsh(); cmd[1] = node.trim();
		 * cmd[2] =
		 * rubis.getMonitoringProgram()+" "+rubis.getMonitoringOptions()+" "+
		 * rubis.getMonitoringSampling()+" "+fullTimeInSec+" > "+outputFileName;
		 * System.out.println("&nbsp &nbsp Command is: "+cmd[0]+" "+cmd[1]+" "
		 * +cmd[2]+"<br>\n");
		 * System.err.println("startMonitoringProgram Command is: "+cmd[0]+" "
		 * +cmd[1]+" "+cmd[2]); return Runtime.getRuntime().exec(cmd); } catch
		 * (IOException ioe) { System.out.
		 * println("An error occured while executing monitoring program ("+ioe.
		 * getMessage()+")"); return null; }
		 */
		return null;
	}

	/**
	 * Run the node_info.sh script on the remote node and just forward what we
	 * get from standard output.
	 *
	 * @param node
	 *            node to get information from
	 */
	private void printNodeInformation(String node) {
		try {
			File dir = new File(".");
			String nodeInfoProgram = dir.getCanonicalPath() + "/bench/node_info.sh";

			String[] cmd = new String[3];
			cmd[0] = rubis.getMonitoringRsh();
			cmd[1] = node;
			cmd[2] = nodeInfoProgram;
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader read = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String msg;
			while ((msg = read.readLine()) != null)
				System.out.println(msg + "<br>");
			read.close();
		} catch (Exception ioe) {
			System.out.println("An error occured while getting node information (" + ioe.getMessage() + ")");
		}
	}

	/**
	 * Main program take an optional output file argument only if it is run on
	 * as a remote client.
	 *
	 * @param args
	 *            optional output file if run as remote client
	 * @throws InterruptedException
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws InterruptedException, FileNotFoundException {
		GregorianCalendar startDate;
		GregorianCalendar endDate;
		GregorianCalendar upRampDate;
		GregorianCalendar runSessionDate;
		GregorianCalendar downRampDate;
		GregorianCalendar endDownRampDate;
		Process webServerMonitor = null;
		Process[] dbServerMonitor = null;
		Process[] ejbServerMonitor = null;
		Process[] servletsServerMonitor = null;
		Process clientMonitor;
		Process[] remoteClientMonitor = null;
		Process[] remoteClient = null;
		String reportDir = "";
		boolean isMainClient = (args.length <= 3); // Check if we are the main
													// client
		String propertiesFileName;

		// String rawLogFileName = "/home/hcnguye3/rawlog.txt";
		if (isMainClient) {
			// Start by creating a report directory and redirecting output to an
			// index.html file
			System.out.println("RUBiS client emulator - (C) Rice University/INRIA 2001\n");
			// if (args.length <= 1)
			// {
			reportDir = "bench/" + TimeManagement.currentDateToString() + "/";
			reportDir = reportDir.replace(' ', '@');
			// }
			// else
			// {
			// reportDir = "bench/"+args[1];
			// }
			try {
				System.out.println("Creating report directory " + reportDir);
				File dir = new File(reportDir);
				dir.mkdirs();
				if (!dir.isDirectory()) {
					System.out.println("Unable to create " + reportDir + " using current directory instead");
					reportDir = "./";
				} else
					reportDir = dir.getCanonicalPath() + "/";
				// System.out.println("Redirecting output to
				// '"+reportDir+"index.html'");
				// PrintStream outputStream = new PrintStream(new
				// FileOutputStream(reportDir+"index.html"));
				System.out.println("Please wait while experiment is running ...");
				// System.setOut(outputStream);
				// System.setErr(outputStream);
			} catch (Exception e) {
				System.out.println(
						"Output redirection failed, displaying results on standard output (" + e.getMessage() + ")");
			}
			System.out.println("RUBiS client emulator - (C) Rice University/INRIA 2001");
			startDate = new GregorianCalendar();
			System.out.println("Test date: " + TimeManagement.dateToString(startDate));

			if (args.length == 0)
				propertiesFileName = "rubis";
			else
				propertiesFileName = "rubis";
		} else {
			System.out.println("RUBiS remote client emulator - (C) Rice University/INRIA 2001");
			startDate = new GregorianCalendar();
			propertiesFileName = args[2];
		}

		WorkloadGenerator client = new WorkloadGenerator(propertiesFileName); // Get
																				// also
																				// rubis.properties
																				// info

		if (args.length > 1) {
			//int numOfClinets = Integer.parseInt(args[1]);
			// client.rubis.setNbOfClients(numOfClinets);
		}
		if (args.length > 2) {
			// rawLogFileName = args[2];
		}

		Stats stats = new Stats(client.rubis.getNbOfRows());
		Stats upRampStats = new Stats(client.rubis.getNbOfRows());
		Stats runSessionStats = new Stats(client.rubis.getNbOfRows());
		Stats downRampStats = new Stats(client.rubis.getNbOfRows());
		Stats allStats = new Stats(client.rubis.getNbOfRows());
//		UserSession[] sessions = new UserSession[client.rubis.getNbOfClients()];
//
//		System.err.println("WorkloadGenerator initialized for rubis server: " + client.rubis.getWebServerName());
//
//		// #############################
//		// ### TEST TRACE BEGIN HERE ###
//		// #############################
//
//		System.err.println("Run user sessions, total number: " + client.rubis.getNbOfClients());
//
//		// Run user sessions
//		System.out.println("WorkloadGenerator: Starting " + client.rubis.getNbOfClients() + " session threads");
//		for (int i = 0; i < client.rubis.getNbOfClients(); i++) {
//			sessions[i] = new UserSession("UserSession" + i, client.urlGen, client.rubis, stats);
//			// sessions[i].rawLogFileName = rawLogFileName;
//			sessions[i].start();
//		}
//
//		System.err.println("All user sessions have started: " + client.rubis.getNbOfClients());

		// System.err.println("Start up-ramp");
		//
		// // Start up-ramp
		//
		// System.out.println("WorkloadGenerator: Switching to ** UP RAMP **");
		// client.setSlowDownFactor(client.rubis.getUpRampSlowdown());
		// upRampDate = new GregorianCalendar();
		//
		// System.err.println("Begin to sleep: " +
		// client.rubis.getUpRampTime());
		//
		// try {
		// Thread.currentThread().sleep(client.rubis.getUpRampTime());
		// } catch (java.lang.InterruptedException ie) {
		// System.err.println("WorkloadGenerator has been interrupted.");
		// }
		// upRampStats.merge(stats);
		// stats.reset(); // Note that as this is not
		// // atomic we may lose some stats here ...
		//
		// System.err.println("Start runtime session");
		//
		// // Start runtime session
		// System.out.println("<br><A NAME=\"run\"></A>");
		// System.out.println("<h3>WorkloadGenerator: Switching to ** RUNTIME
		// SESSION **</h3><br><p>");
		// client.setSlowDownFactor(1);
		// runSessionDate = new GregorianCalendar();
		// System.err.println("Begin to sleep: " +
		// client.rubis.getSessionTime());
		// try {
		// Thread.currentThread().sleep(client.rubis.getSessionTime());
		// } catch (java.lang.InterruptedException ie) {
		// System.err.println("WorkloadGenerator has been interrupted.");
		// }
		// runSessionStats.merge(stats);
		// stats.reset(); // Note that as this is
		// // not atomic we may lose some stats here ...
		//
		// System.err.println("Start down-ramp");
		//
		// // Start down-ramp System.out.println("<br><A NAME=\"down\"></A>");
		// System.out.println("<h3>WorkloadGenerator: Switching to ** DOWN RAMP
		// **</h3><br><p>");
		// client.setSlowDownFactor(client.rubis.getDownRampSlowdown());
		// downRampDate = new GregorianCalendar();
		// System.err.println("Begin to sleep: " +
		// client.rubis.getDownRampTime());
		// try {
		// Thread.currentThread().sleep(client.rubis.getDownRampTime());
		// } catch (java.lang.InterruptedException ie) {
		// System.err.println("WorkloadGenerator has been interrupted.");
		// }
		// downRampStats.merge(stats);
		// endDownRampDate = new GregorianCalendar();
		//
		// System.err.println("WorkloadGenerator: Shutting down threads ...");
		//
		// // Wait for completion client.setEndOfSimulation();
		// System.out.println("WorkloadGenerator: Shutting down threads
		// ...<br>");
		// for (int i = 0; i < client.rubis.getNbOfClients(); i++) {
		// try {
		// long t = 5000 - 1000 * i;
		// if (t <= 0)
		// t = 10;
		// sessions[i].join(t);
		// } catch (java.lang.InterruptedException ie) {
		// System.err.println("WorkloadGenerator: Thread " + i + " has been
		// interrupted.");
		// }
		// }
		
		int timeToSleep = Integer.parseInt(args[1]);
		
		File file = new File(args[0]);
		Scanner sc = new Scanner(file);
		
		while (sc.hasNextInt()) {
			
			int numberOfClients = sc.nextInt();
			client.endOfSimulation = false;

			UserSession[] sessions = new UserSession[numberOfClients];

			// System.err.println("WorkloadGenerator initialized for rubis
			// server: " + client.rubis.getWebServerName());

			// System.err.println("Run user sessions, total number: " +
			// client.rubis.getNbOfClients());

			// Run user sessions
			System.out.println("WorkloadGenerator: Starting " + numberOfClients + " session threads");
			for (int i = 0; i < numberOfClients; i++) {
				sessions[i] = new UserSession("UserSession" + i, client.urlGen, client.rubis, stats);
				// sessions[i].rawLogFileName = rawLogFileName;
				sessions[i].start();
			}

			Thread.sleep(timeToSleep);
			
			client.setEndOfSimulation();

			for (int i = 0; i < numberOfClients; i++) {
				try {
					sessions[i].join();
				} catch (java.lang.InterruptedException ie) {
					System.err.println("WorkloadGenerator: Thread " + i + " has been interrupted.");
				}
			}
			System.out.println("All User Sessions have finished. We will start next set of clients");
		}

		System.out.println("Done\n");
		endDate = new GregorianCalendar();
		allStats.merge(stats);
		allStats.merge(runSessionStats);
		allStats.merge(upRampStats);

	}

}
