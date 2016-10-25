package org.seh;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.schneiderautomation.factorycast.CommBean;
import com.schneiderautomation.factorycast.comm.xbf.ENetStatistics;
import com.schneiderautomation.factorycast.comm.xbf.ENetStats;
import com.schneiderautomation.factorycast.comm.xbf.ENetStatsExt;
import com.schneiderautomation.misc.GlobalConfig;

import ftp.FtpBean;

public class EnetApplet extends Applet implements Runnable {
	private static final long serialVersionUID = 1L;

	private CommBean comm;
	private ENetStatistics enet;
	FtpBean ftp = null;
	int ftpRequests = 0;
	String ftpDirectory = "/SDCA/WEB/wwwroot";
	private Thread engine;
	private String ipHost;
	TextArea result = new TextArea("Write Command to see results here...", 20, 50, TextArea.SCROLLBARS_VERTICAL_ONLY );

	public void init() {
		GlobalConfig.setLanguage(getParameter("Language"));
		setLocale(GlobalConfig.getLocale());
		EnetStrings.init(getLocale());

		this.comm = new CommBean(getLocale());
		ipHost = getParameter("htmlhost");
		if (ipHost == null) {
			ipHost = getCodeBase().getHost();
		}

		try {
			showStatus(EnetStrings.getString("CONNECTWAIT"));
			this.comm.connect(ipHost, true);
			
			this.enet = ((ENetStatistics) this.comm.getServer().getExtension(ENetStatistics.class));
		} catch (Exception localException) {
			localException.printStackTrace();
		}
		GridBagConstraints localGridBagConstraints = new GridBagConstraints();
		GridBagLayout localGridBagLayout = new GridBagLayout();
		setBackground(Color.white);
		setLayout(localGridBagLayout);
		localGridBagConstraints.fill = GridBagConstraints.BOTH;
		localGridBagConstraints.gridwidth = 1;
		localGridBagConstraints.gridheight = 1;
		localGridBagConstraints.gridx = 0;
		localGridBagConstraints.gridy = 0;
		localGridBagConstraints.weightx = 1.0;
		String title;
		try {
			title = "Server Software Version: "+this.comm.getServer().getProgramInfo().toDisplayString();
		} catch (Exception e) {
			title = e.getLocalizedMessage();
		}
		
		Panel localPanel = new Panel();
		Label titleLabel = new Label(title, 1);
		titleLabel.setFont(new Font("Arial", 1, 16));
		localPanel.add(titleLabel);
		localGridBagLayout.setConstraints(localPanel, localGridBagConstraints);
		add(localPanel);

		localGridBagConstraints.gridy += 1;
		TextField commandTextField = new TextField(100);
		commandTextField.setEditable(true);
		commandTextField.addActionListener(new RunCommandActionListener());
		localGridBagLayout.setConstraints(commandTextField, localGridBagConstraints);
		add(commandTextField);
		
		localGridBagConstraints.gridy += 1;
		localGridBagConstraints.weighty = 1.0;
		result.setEditable(false);
		result.setBackground(Color.WHITE);
		localGridBagLayout.setConstraints(result, localGridBagConstraints);
		add(result);
		localGridBagConstraints.weighty = 0.0;
		localGridBagConstraints.gridy += 1;

		StringBuffer sb = new StringBuffer();
		sb.append("<html>");
		sb.append("<b>Command List:</b> IP - SUBNETMASK - GATEWAY - STATUS - MODEL - MAC - RACK - MODULE - CLEAR - LS - CD<br><br>");
		sb.append("</html>");
		
		JLabel help = new JLabel(sb.toString(), SwingConstants.LEFT);
		localGridBagLayout.setConstraints(help, localGridBagConstraints);
		
		add(help);
	}

	public void destroy() {
		try {
			this.comm.disconnect();
		} catch (Exception localException) {
			localException.printStackTrace();
		}
	}

	private void startMonitoring() {
		if (this.engine == null) {
			this.engine = new Thread(this);
			this.engine.start();
		}
	}

	private void stopMonitoring() {
		Thread localThread = null;
		synchronized (this) {
			localThread = this.engine;
			this.engine = null;
			notify();
		}
		if (localThread != null) {
			try {
				localThread.join();
			} catch (InterruptedException localInterruptedException) {
			}
		}
	}

	public synchronized void run() {
		while (this.engine!=null) {
			EnetApplet.this.result.append(".");
			try {
				wait(1000L);
			} catch (InterruptedException localInterruptedException) {}
		}
	}

	class RunCommandActionListener implements ActionListener {
		RunCommandActionListener() {}

		public void actionPerformed(ActionEvent e) {
			try {
				TextField commandField = (TextField)e.getSource();
				String command = commandField.getText();
				commandField.setText("");
				EnetApplet.this.result.append("\n"+command);
				startMonitoring();
				
				String response = "Unknow Command";
				
				ENetStats localENetStats;
				ENetStatsExt localENetStatsExt;

				if("CLEAR".equalsIgnoreCase(command)){
					response = "";
					EnetApplet.this.result.setText("");
				}else try {
					localENetStats = EnetApplet.this.enet.getNetworkStatistics();
					localENetStatsExt = EnetApplet.this.enet.getNetworkStatisticsExt();

					if("IP".equalsIgnoreCase(command)){
						String ipHost = getParameter("htmlhost");
						if (ipHost == null) ipHost = getCodeBase().getHost();
						InetAddress localInetAddress = InetAddress.getByName( ipHost );
						response = "Canonical: "+localInetAddress.getCanonicalHostName()+
								"\nHostAddress: " + localInetAddress.getHostAddress()+
								"\nHostName: " + localInetAddress.getHostName();
					}

					if("STATUS".equalsIgnoreCase(command)){
						int i = localENetStats.moduleStatus;
						int j = (i & 0xFF0) >> 4;
						if ((i & 0x1) != 0) {
							String localObject = EnetStrings.getString("RUNNING");
							if ((j >= 17) && (j <= 19)) {
								if ((i & 0x8000) != 0) {
									localObject = ((String) localObject).concat(EnetStrings
											.getString("LINK"));
								}
								if ((i & 0x4000) != 0) {
									localObject = ((String) localObject).concat(EnetStrings
											.getString("APPL"));
								}
							}
							response = (String) localObject;
						} else {
							response = EnetStrings.getString("STOPPED");
						}
					}

					if("MODEL".equalsIgnoreCase(command)){
						int i = localENetStats.moduleStatus;
						int j = (i & 0xFF0) >> 4;
						switch (j) {
						case 17:
							response = "BMX P34 CPU";
							break;
						case 18:
							response = "BMX NOE 0100";
							break;
						case 19:
							response = "BMX NOE 0110";
							break;
						default:
							response = "UNKNOWN";
						}
					}

					if("SPEED".equalsIgnoreCase(command)){
						int i = localENetStats.moduleStatus;
						if ((i & 0x1000) != 0) {
							response = "100 MB";
						} else {
							response = "10 MB";
						}
					}
					
					if("MAC".equalsIgnoreCase(command)){
						int i = localENetStats.moduleStatus;
						int j = (i & 0xFF0) >> 4;
					
						StringBuffer localStringBuffer = new StringBuffer();
						for (int k = 0; k < localENetStats.macAddress.length; k++) {
							String str = Integer.toHexString(localENetStats.macAddress[k]);
							if (str.length() == 1) {
								str = "0" + str;
							}
							localStringBuffer.append(str);
							localStringBuffer.append(" ");
						}
						response = localStringBuffer.toString();
					}
					
					if("Unknow Command".equalsIgnoreCase(response)){
						int i = localENetStats.moduleStatus;
						int j = (i & 0xFF0) >> 4;
						if (localENetStatsExt != null) {
							if("RACK".equalsIgnoreCase(command)){
								response = Integer.toString(localENetStatsExt.rack);
							}
							if("MODULE".equalsIgnoreCase(command)){
								response = Integer.toString(localENetStatsExt.module);
							}
							if("SUBNETMASK".equalsIgnoreCase(command)){
								response = localENetStatsExt.ipMask[3] + "."
										+ localENetStatsExt.ipMask[2] + "."
										+ localENetStatsExt.ipMask[1] + "."
										+ localENetStatsExt.ipMask[0];
							}
							if("GATEWAY".equalsIgnoreCase(command)){
								response = localENetStatsExt.ipGateway[3] + "."
										+ localENetStatsExt.ipGateway[2] + "."
										+ localENetStatsExt.ipGateway[1] + "."
										+ localENetStatsExt.ipGateway[0];
							}
							
						}
						
					}
					if("Unknow Command".equalsIgnoreCase(response)){
						if(ftp==null){
							ftp = new FtpBean();
							ftp.setSocketTimeout(60000);

							EnetApplet.this.result.append(" connecting to "+ipHost+" ["+ftp.getSystemType()+"]");
							ftp.ftpConnect(ipHost, "sysdiag", "factorycast@schneider", ftpDirectory);
						}
						
						if("LS".equalsIgnoreCase(command)){
							ftp.setDirectory(ftpDirectory);
							response = "listing directory: "+ftpDirectory+"\n"+ftp.getDirectory()+"\n"+ftp.getDirectoryContentAsString()+"\n";
						}
						if(command.startsWith("CD") || command.startsWith("cd")){
							ftp.setDirectory(command.split(" ")[1]);
							ftpDirectory = ftp.getDirectory();
							response = "current directory: "+ftpDirectory;
						}

						if(command.startsWith("GET") || command.startsWith("get")){
							EnetApplet.this.result.append("getting: "+ftpDirectory+"/"+command.split(" ")[1]);
							byte[] fileGetted = ftp.getBinaryFile(command.split(" ")[1]);
							response = "done!";
						}

						//must reset ftp connection to clean session
						ftpRequests++;
						if( ftpRequests == 3 ){
							ftp.close();
							ftpRequests = 0;
							ftp = null;
						}
						
					}

					
				} catch (Exception localException) {
					response = "Communication Error! "+localException.getLocalizedMessage();
				}

				
				stopMonitoring();
				EnetApplet.this.result.append("\n>"+response);

			} catch (Exception localException) {
				localException.printStackTrace();
			}
		}
	}
	
}
