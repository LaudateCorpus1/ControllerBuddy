package de.bwravencl.RemoteStick.gui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import de.bwravencl.RemoteStick.ServerThread;
import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.Profile;

import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.GridLayout;
import java.io.File;
import java.util.List;
import java.util.UUID;

import javax.swing.JSpinner;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;

import javax.swing.border.MatteBorder;

public class Main {

	public static final long ASSIGNMENTS_PANEL_UPDATE_RATE = 100L;

	private Input input;
	private ServerThread serverThread;

	private JFrame frmRemotestickserver;
	private JTabbedPane tabbedPane;
	private JScrollPane scrollPaneAssignments;
	private JMenu mnController;
	private JPanel panelProfiles;
	private JPanel panelAssignments;
	private JSpinner spinnerPort;
	private JSpinner spinnerClientTimeout;
	private JSpinner spinnerUpdateRate;

	private boolean suspendControllerSettingsUpdate = false;

	private final ButtonGroup buttonGroupServerState = new ButtonGroup();
	private final Action openFileAction = new OpenFileAction();
	private final Action saveFileAction = new SaveFileAction();
	private final Action quitAction = new QuitAction();
	private final Action startServerAction = new StartServerAction();
	private final Action stopServerAction = new StopServerAction();
	private final JFileChooser jFileChooser = new JFileChooser();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main window = new Main();
					window.frmRemotestickserver.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Main() {
		initialize();

		final Controller[] controllers = ControllerEnvironment
				.getDefaultEnvironment().getControllers();
		for (Controller c : controllers)
			if (c.getType() != Type.KEYBOARD && c.getType() != Type.MOUSE
					&& c.getType() != Type.TRACKBALL
					&& c.getType() != Type.TRACKPAD) {
				input = new Input(c);
				break;
			}

		updateProfilesPanel();

		final Thread updateAssignmentsPanelThread = new UpdateAssignmentsPanelThread();
		updateAssignmentsPanelThread.start();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmRemotestickserver = new JFrame();
		frmRemotestickserver.setTitle("RemoteStick Server");
		frmRemotestickserver.setBounds(100, 100, 650, 600);
		frmRemotestickserver.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final JMenuBar menuBar = new JMenuBar();
		frmRemotestickserver.setJMenuBar(menuBar);

		final JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		mnFile.add(openFileAction);
		mnFile.add(saveFileAction);
		mnFile.add(new JSeparator());
		mnFile.add(quitAction);

		mnController = new JMenu("Controller");
		mnController.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				mnController.removeAll();

				final Controller[] controllers = ControllerEnvironment
						.getDefaultEnvironment().getControllers();

				for (Controller c : controllers)
					if (c.getType() != Type.KEYBOARD
							&& c.getType() != Type.MOUSE
							&& c.getType() != Type.TRACKBALL
							&& c.getType() != Type.TRACKPAD)
						mnController.add(new SelectControllerAction(c));
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}
		});
		mnController.setEnabled(true);
		menuBar.add(mnController);

		final JMenu mnServer = new JMenu("Server");
		menuBar.add(mnServer);

		final JRadioButtonMenuItem rdbtnmntmRun = new JRadioButtonMenuItem(
				"Run");
		rdbtnmntmRun.setAction(startServerAction);
		buttonGroupServerState.add(rdbtnmntmRun);
		mnServer.add(rdbtnmntmRun);

		final JRadioButtonMenuItem rdbtnmntmStop = new JRadioButtonMenuItem(
				"Stop");
		rdbtnmntmStop.setAction(stopServerAction);
		rdbtnmntmStop.setSelected(true);
		buttonGroupServerState.add(rdbtnmntmStop);
		mnServer.add(rdbtnmntmStop);
		frmRemotestickserver.getContentPane().setLayout(
				new BoxLayout(frmRemotestickserver.getContentPane(),
						BoxLayout.X_AXIS));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmRemotestickserver.getContentPane().add(tabbedPane);

		final JScrollPane scrollPaneProfiles = new JScrollPane();
		tabbedPane.addTab("Profiles", null, scrollPaneProfiles, null);
		panelProfiles = new JPanel();
		scrollPaneProfiles.setViewportView(panelProfiles);
		panelProfiles.setLayout(new GridLayout(0, 3, 10, 5));
		scrollPaneProfiles.setViewportBorder(new MatteBorder(10, 10, 0, 10,
				panelProfiles.getBackground()));

		scrollPaneAssignments = new JScrollPane();
		tabbedPane.addTab("Assignments", null, scrollPaneAssignments, null);

		panelAssignments = new JPanel();
		scrollPaneAssignments.setViewportView(panelAssignments);
		panelAssignments.setLayout(new GridLayout(0, 3, 10, 5));
		scrollPaneAssignments.setViewportBorder(new MatteBorder(10, 10, 0, 10,
				panelAssignments.getBackground()));

		final JPanel panelServerSettings = new JPanel();
		panelServerSettings.setBorder(new MatteBorder(10, 10, 10, 10,
				panelServerSettings.getBackground()));
		tabbedPane.addTab("Server Settings", null, panelServerSettings, null);
		panelServerSettings.setLayout(new GridLayout(0, 2, 10, 50));

		final JLabel lblPort = new JLabel("Port");
		panelServerSettings.add(lblPort);

		spinnerPort = new JSpinner(new SpinnerNumberModel(
				ServerThread.DEFAULT_PORT, 1024, 65535, 1));
		panelServerSettings.add(spinnerPort);

		final JLabel lblClientTimeout = new JLabel("Client Timeout");
		panelServerSettings.add(lblClientTimeout);

		spinnerClientTimeout = new JSpinner(new SpinnerNumberModel(
				ServerThread.DEFAULT_CLIENT_TIMEOUT, 10, 60000, 1));
		panelServerSettings.add(spinnerClientTimeout);

		final JLabel lblUpdateRate = new JLabel("Update Rate");
		panelServerSettings.add(lblUpdateRate);

		spinnerUpdateRate = new JSpinner(new SpinnerNumberModel(
				(int) ServerThread.DEFAULT_UPDATE_RATE, 1, 1000, 1));
		panelServerSettings.add(spinnerUpdateRate);

		panelServerSettings.add(Box.createGlue());
		panelServerSettings.add(Box.createGlue());
		panelServerSettings.add(Box.createGlue());
		panelServerSettings.add(Box.createGlue());
		panelServerSettings.add(Box.createGlue());
		panelServerSettings.add(Box.createGlue());
		panelServerSettings.add(Box.createGlue());
		panelServerSettings.add(Box.createGlue());

		final FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Controller Profiles", "json");
		jFileChooser.setFileFilter(filter);
	}

	private void updateProfilesPanel() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				panelProfiles.removeAll();

				final List<Profile> profiles = input.getProfiles();
				for (Profile p : profiles) {
					final JTextField textFieldDescription = new JTextField(p
							.getDescription());
					if (p.getUuid()
							.equals(UUID
									.fromString(Profile.DEFAULT_PROFILE_UUID_STRING)))
						textFieldDescription.setEnabled(false);
					else {
						final SetProfileDescriptionAction setProfileDescriptionAction = new SetProfileDescriptionAction(
								p, textFieldDescription);
						textFieldDescription
								.addActionListener(setProfileDescriptionAction);
						textFieldDescription
								.addFocusListener(setProfileDescriptionAction);
					}
					panelProfiles.add(textFieldDescription);
				}

				panelProfiles.validate();
			}
		});
	}

	private void stopServer() {
		if (serverThread != null)
			serverThread.stopServer();
	}

	private class OpenFileAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public OpenFileAction() {
			putValue(NAME, "Open");
			putValue(SHORT_DESCRIPTION,
					"Loads a controller configuration from a file");
		}

		public void actionPerformed(ActionEvent e) {
			if (jFileChooser.showOpenDialog(frmRemotestickserver) == JFileChooser.APPROVE_OPTION) {
				final File file = jFileChooser.getSelectedFile();
			}
		}
	}

	private class SaveFileAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public SaveFileAction() {
			putValue(NAME, "Save");
			putValue(SHORT_DESCRIPTION,
					"Saves the controller configuration to a file");
		}

		public void actionPerformed(ActionEvent e) {
			if (jFileChooser.showSaveDialog(frmRemotestickserver) == JFileChooser.APPROVE_OPTION) {
				final File file = jFileChooser.getSelectedFile();
			}
		}
	}

	private class QuitAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public QuitAction() {
			putValue(NAME, "Quit");
			putValue(SHORT_DESCRIPTION, "Quits the application");
		}

		public void actionPerformed(ActionEvent e) {
			stopServer();
			System.exit(0);
		}
	}

	private class StartServerAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public StartServerAction() {
			putValue(NAME, "Start");
			putValue(SHORT_DESCRIPTION, "Starts the server");
		}

		public void actionPerformed(ActionEvent e) {
			serverThread = new ServerThread(input);
			serverThread.setPort((int) spinnerPort.getValue());
			serverThread
					.setClientTimeout((int) spinnerClientTimeout.getValue());
			serverThread.setUpdateRate((int) spinnerUpdateRate.getValue());
			serverThread.start();
		}
	}

	private class StopServerAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public StopServerAction() {
			putValue(NAME, "Stop");
			putValue(SHORT_DESCRIPTION, "Stops the server");
		}

		public void actionPerformed(ActionEvent e) {
			stopServer();
		}
	}

	private class SelectControllerAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final Controller controller;

		public SelectControllerAction(Controller controller) {
			this.controller = controller;

			final String name = controller.getName();
			putValue(NAME, name);
			putValue(SHORT_DESCRIPTION, "Selects '" + name
					+ "' as the active controller");
		}

		public void actionPerformed(ActionEvent e) {
			input = new Input(controller);
		}
	}

	private class UpdateAssignmentsPanelThread extends Thread {

		@Override
		public void run() {
			super.run();

			while (true) {
				if (!suspendControllerSettingsUpdate
						&& scrollPaneAssignments.equals(tabbedPane
								.getSelectedComponent())
						&& frmRemotestickserver.getState() != Frame.ICONIFIED)
					EventQueue.invokeLater(new Runnable() {

						@Override
						public void run() {

							panelAssignments.removeAll();
							panelAssignments.add(new JLabel("Controller: "));

							final Controller controller = input.getController();
							if (controller != null) {
								panelAssignments.add(new JLabel(controller
										.getName()));
								panelAssignments.add(Box.createGlue());

								panelAssignments.add(new JSeparator());
								panelAssignments.add(new JSeparator());
								panelAssignments.add(new JSeparator());

								controller.poll();

								for (Component c : controller.getComponents()) {
									final String name = c.getName();
									final float value = c.getPollData();

									if (c.isAnalog()) {
										panelAssignments.add(new JLabel(
												"Axis: " + name));

										final JProgressBar jProgressBar = new JProgressBar(
												-100, 100);
										jProgressBar
												.setValue((int) (value * 100.0f));
										panelAssignments.add(jProgressBar);
									} else {
										panelAssignments.add(new JLabel(
												"Button: " + name));

										final JLabel jLabel = new JLabel();
										jLabel.setHorizontalAlignment(SwingConstants.CENTER);
										if (value > 0.5f)
											jLabel.setText("Down");
										else {
											jLabel.setText("Up");
											jLabel.setForeground(Color.LIGHT_GRAY);
										}
										panelAssignments.add(jLabel);
									}

									final JButton editButton = new JButton(
											new EditComponentAction(c));
									editButton
											.addMouseListener(new MouseListener() {

												@Override
												public void mouseReleased(
														MouseEvent e) {
												}

												@Override
												public void mousePressed(
														MouseEvent e) {
													suspendControllerSettingsUpdate = true;
												}

												@Override
												public void mouseExited(
														MouseEvent e) {
												}

												@Override
												public void mouseEntered(
														MouseEvent e) {
												}

												@Override
												public void mouseClicked(
														MouseEvent e) {
												}
											});
									panelAssignments.add(editButton);
								}
							} else
								panelAssignments.add(new JLabel(
										"No active controller selected!"));

							panelAssignments.validate();
						}
					});

				try {
					Thread.sleep(ASSIGNMENTS_PANEL_UPDATE_RATE);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class EditComponentAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final Component component;

		public EditComponentAction(Component component) {
			this.component = component;

			putValue(NAME, "Edit");
			putValue(SHORT_DESCRIPTION,
					"Edit actions of the '" + component.getName()
							+ "' component");
		}

		public void actionPerformed(ActionEvent e) {
			final EditComponentDialog editComponentDialog = new EditComponentDialog(
					input, component);
			editComponentDialog.setVisible(true);

			suspendControllerSettingsUpdate = false;
		}
	}

	private class SetProfileDescriptionAction extends AbstractAction implements
			FocusListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final Profile profile;
		private final JTextField profileDescriptionTextField;

		public SetProfileDescriptionAction(Profile profile,
				JTextField profileDescriptionTextField) {
			this.profile = profile;
			this.profileDescriptionTextField = profileDescriptionTextField;

			putValue(NAME, "Set profile description");
			putValue(SHORT_DESCRIPTION, "Sets the description of a profile");
		}

		public void actionPerformed(ActionEvent e) {
			setProfileDescription();
		}

		@Override
		public void focusGained(FocusEvent e) {
		}

		@Override
		public void focusLost(FocusEvent e) {
			setProfileDescription();
		}

		private void setProfileDescription() {
			final String description = profileDescriptionTextField.getText();

			if (description != null && description.length() > 0)
				profile.setDescription(description);
			else
				profileDescriptionTextField.setText(profile.getDescription());
		}
	}

}
