package de.bwravencl.RemoteStick.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
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

import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.Mode;
import de.bwravencl.RemoteStick.input.Profile;
import de.bwravencl.RemoteStick.input.action.IAction;
import de.bwravencl.RemoteStick.net.ServerThread;

import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JLabel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;

public class Main {

	public static final String PROFILE_FILE_EXTENSION = "json";
	public static final String PROFILE_FILE_SUFFIX = '.' + PROFILE_FILE_EXTENSION;

	public static final long ASSIGNMENTS_PANEL_UPDATE_RATE = 100L;
	public static final Dimension BUTTON_DIMENSION = new Dimension(100, 25);

	public static final String PREFERENCES_LAST_PROFILE = "last_profile";
	public static final String PREFERENCES_PORT = "port";
	public static final String PREFERENCES_CLIENT_TIMEOUT = "client_timeout";
	public static final String PREFERENCES_UPDATE_RATE = "update_rate";

	private JFrame frmRemoteStickServer;
	private JTabbedPane tabbedPane;
	private JScrollPane scrollPaneAssignments;
	private JMenu mnController;
	private JPanel panelModeList;
	private JPanel panelAssignments;
	private JSpinner spinnerPort;
	private JSpinner spinnerClientTimeout;
	private JSpinner spinnerUpdateRate;
	private JScrollPane scrollPaneModes;
	private final JFileChooser fileChooser = new JFileChooser();

	private boolean suspendControllerSettingsUpdate = false;
	private Input input;
	private ServerThread serverThread;
	private final Preferences preferences = Preferences.userNodeForPackage(this
			.getClass());

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main window = new Main();
					window.frmRemoteStickServer.setVisible(true);
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

		updateModesPanel();

		final Thread updateAssignmentsPanelThread = new UpdateAssignmentsPanelThread();
		updateAssignmentsPanelThread.start();

		final String path = preferences.get(PREFERENCES_LAST_PROFILE, null);
		if (path != null)
			loadProfile(new File(path));
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmRemoteStickServer = new JFrame();
		frmRemoteStickServer.setTitle("RemoteStick Server");
		frmRemoteStickServer.setBounds(100, 100, 650, 600);
		frmRemoteStickServer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final JMenuBar menuBar = new JMenuBar();
		frmRemoteStickServer.setJMenuBar(menuBar);

		final JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		mnFile.add(new OpenFileAction());
		mnFile.add(new SaveFileAction());
		mnFile.add(new JSeparator());
		mnFile.add(new QuitAction());

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

		final ButtonGroup buttonGroupServerState = new ButtonGroup();

		final JRadioButtonMenuItem rdbtnmntmRun = new JRadioButtonMenuItem(
				"Run");
		rdbtnmntmRun.setAction(new StartServerAction());
		buttonGroupServerState.add(rdbtnmntmRun);
		mnServer.add(rdbtnmntmRun);

		final JRadioButtonMenuItem rdbtnmntmStop = new JRadioButtonMenuItem(
				"Stop");
		rdbtnmntmStop.setAction(new StopServerAction());
		rdbtnmntmStop.setSelected(true);
		buttonGroupServerState.add(rdbtnmntmStop);
		mnServer.add(rdbtnmntmStop);
		frmRemoteStickServer.getContentPane().setLayout(
				new BoxLayout(frmRemoteStickServer.getContentPane(),
						BoxLayout.X_AXIS));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmRemoteStickServer.getContentPane().add(tabbedPane);

		final JPanel panelModes = new JPanel(new BorderLayout());
		tabbedPane.addTab("Modes", null, panelModes, null);

		panelModeList = new JPanel();
		panelModeList.setLayout(new GridBagLayout());

		scrollPaneModes = new JScrollPane();
		scrollPaneModes.setViewportBorder(BorderFactory.createMatteBorder(10,
				10, 0, 10, panelModeList.getBackground()));
		panelModes.add(scrollPaneModes, BorderLayout.CENTER);

		final JPanel panelAddMode = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton addButton = new JButton(new AddModeAction());
		addButton.setPreferredSize(BUTTON_DIMENSION);
		panelAddMode.add(addButton);
		panelModes.add(panelAddMode, BorderLayout.SOUTH);

		panelAssignments = new JPanel();
		panelAssignments.setLayout(new GridBagLayout());

		scrollPaneAssignments = new JScrollPane();
		scrollPaneAssignments.setViewportBorder(BorderFactory
				.createMatteBorder(10, 10, 0, 10,
						panelAssignments.getBackground()));
		tabbedPane.addTab("Assignments", null, scrollPaneAssignments, null);

		final JPanel panelServerSettings = new JPanel();
		panelServerSettings.setLayout(new GridBagLayout());

		final JScrollPane scrollPaneServerSettings = new JScrollPane();
		scrollPaneServerSettings.setViewportView(panelServerSettings);
		/*
		 * scrollPaneServerSettings.setViewportBorder(BorderFactory
		 * .createMatteBorder(10, 10, 0, 10,
		 * panelServerSettings.getBackground()));
		 */
		tabbedPane.addTab("Server Settings", null, scrollPaneServerSettings,
				null);

		final GridBagConstraints panelGridBagConstraints = new GridBagConstraints(
				0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 5);

		final FlowLayout panelFlowLayout = new FlowLayout(FlowLayout.LEADING,
				10, 10);

		final JPanel panelPort = new JPanel(panelFlowLayout);
		panelServerSettings.add(panelPort, panelGridBagConstraints);

		final JLabel lblPort = new JLabel("Port");
		lblPort.setPreferredSize(new Dimension(100, 15));
		panelPort.add(lblPort);

		spinnerPort = new JSpinner(new SpinnerNumberModel(preferences.getInt(
				PREFERENCES_PORT, ServerThread.DEFAULT_PORT), 1024, 65535, 1));
		spinnerPort.setEditor(new JSpinner.NumberEditor(spinnerPort, "#"));
		spinnerPort.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_PORT,
						(int) ((JSpinner) e.getSource()).getValue());
			}
		});
		panelPort.add(spinnerPort);

		final JPanel panelTimeout = new JPanel(panelFlowLayout);
		panelServerSettings.add(panelTimeout, panelGridBagConstraints);

		final JLabel lblClientTimeout = new JLabel("Client Timeout");
		lblClientTimeout.setPreferredSize(new Dimension(100, 15));
		panelTimeout.add(lblClientTimeout);

		spinnerClientTimeout = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_CLIENT_TIMEOUT,
						ServerThread.DEFAULT_CLIENT_TIMEOUT), 10, 60000, 1));
		spinnerClientTimeout.setEditor(new JSpinner.NumberEditor(
				spinnerClientTimeout, "#"));
		spinnerClientTimeout.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_CLIENT_TIMEOUT,
						(int) ((JSpinner) e.getSource()).getValue());
			}
		});
		panelTimeout.add(spinnerClientTimeout);

		final JPanel panelUpdateRate = new JPanel(panelFlowLayout);
		panelServerSettings.add(panelUpdateRate, panelGridBagConstraints);

		final JLabel lblUpdateRate = new JLabel("Update Rate");
		lblUpdateRate.setPreferredSize(new Dimension(100, 15));
		panelUpdateRate.add(lblUpdateRate);

		spinnerUpdateRate = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_UPDATE_RATE,
						(int) ServerThread.DEFAULT_UPDATE_RATE), 1, 1000, 1));
		spinnerUpdateRate.setEditor(new JSpinner.NumberEditor(
				spinnerUpdateRate, "#"));
		spinnerUpdateRate.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_UPDATE_RATE,
						(int) ((JSpinner) e.getSource()).getValue());
			}
		});
		panelUpdateRate.add(spinnerUpdateRate);

		panelServerSettings.add(Box.createGlue(), new GridBagConstraints(0,
				GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));

		final FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"RemoteStick Profile", PROFILE_FILE_EXTENSION);
		fileChooser.setFileFilter(filter);
		fileChooser.setSelectedFile(new File(PROFILE_FILE_SUFFIX));
	}

	private void updateModesPanel() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				panelModeList.removeAll();

				final List<Mode> modes = Input.getProfile().getModes();
				for (Mode p : modes) {
					final JPanel panelMode = new JPanel(new GridBagLayout());
					panelModeList.add(panelMode, new GridBagConstraints(0,
							GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
							GridBagConstraints.FIRST_LINE_START,
							GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0,
									0), 0, 5));

					final JLabel lblModeNo = new JLabel("Mode "
							+ modes.indexOf(p));
					lblModeNo.setPreferredSize(new Dimension(100, 15));
					panelMode.add(lblModeNo, new GridBagConstraints(0, 0, 1, 1,
							0.0, 0.0, GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					panelMode.add(Box.createGlue(), new GridBagConstraints(1,
							GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
							GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					final JTextField textFieldDescription = new JTextField(p
							.getDescription(), 20);
					panelMode.add(textFieldDescription, new GridBagConstraints(
							2, 0, 1, 1, 1.0, 1.0, GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					final SetModeDescriptionAction setModeDescriptionAction = new SetModeDescriptionAction(
							p, textFieldDescription);
					textFieldDescription
							.addActionListener(setModeDescriptionAction);
					textFieldDescription
							.addFocusListener(setModeDescriptionAction);

					panelMode.add(Box.createGlue(), new GridBagConstraints(3,
							GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
							GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					final JButton deleteButton = new JButton(
							new DeleteModeAction(p));
					deleteButton.setPreferredSize(BUTTON_DIMENSION);
					panelMode.add(deleteButton, new GridBagConstraints(4,
							GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
							GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					if (Profile.isDefaultMode(p)) {
						textFieldDescription.setEditable(false);
						deleteButton.setEnabled(false);
					}

				}

				panelModeList.add(Box.createGlue(), new GridBagConstraints(0,
						GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
						GridBagConstraints.FIRST_LINE_START,
						GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

				scrollPaneModes.setViewportView(panelModeList);
			}
		});
	}

	private void stopServer() {
		if (serverThread != null)
			serverThread.stopServer();
	}

	private void loadProfile(File file) {
		try {
			@SuppressWarnings("resource")
			final String jsonString = new Scanner(file).useDelimiter("\\A")
					.next();
			final Gson gson = new GsonBuilder().registerTypeAdapter(
					IAction.class, new InterfaceAdapter<IAction>()).create();

			final Profile profile = gson.fromJson(jsonString, Profile.class);
			Input.setProfile(profile);
			saveLastProfile(file);

			updateModesPanel();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void saveLastProfile(File file) {
		preferences.put(PREFERENCES_LAST_PROFILE, file.getAbsolutePath());
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
			if (fileChooser.showOpenDialog(frmRemoteStickServer) == JFileChooser.APPROVE_OPTION) {
				final File file = fileChooser.getSelectedFile();

				loadProfile(file);
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
			if (fileChooser.showSaveDialog(frmRemoteStickServer) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				if (!file.getName().toLowerCase().endsWith(PROFILE_FILE_SUFFIX))
					file = new File(file.getAbsoluteFile()
							+ PROFILE_FILE_SUFFIX);

				final Gson gson = new GsonBuilder().registerTypeAdapter(
						IAction.class, new InterfaceAdapter<IAction>())
						.create();
				final String jsonString = gson.toJson(Input.getProfile());

				try (FileOutputStream fos = new FileOutputStream(file)) {
					final Writer writer = new BufferedWriter(
							new OutputStreamWriter(fos));
					writer.write(jsonString);
					writer.flush();
					fos.flush();
					fos.close();

					saveLastProfile(file);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
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
						&& frmRemoteStickServer.getState() != Frame.ICONIFIED)
					EventQueue.invokeLater(new Runnable() {

						@Override
						public void run() {

							panelAssignments.removeAll();

							final Controller controller = input.getController();
							if (controller != null) {
								controller.poll();

								for (Component c : controller.getComponents()) {
									final JPanel panelComponent = new JPanel(
											new GridBagLayout());
									panelAssignments
											.add(panelComponent,
													new GridBagConstraints(
															0,
															GridBagConstraints.RELATIVE,
															1,
															1,
															0.0,
															0.0,
															GridBagConstraints.FIRST_LINE_START,
															GridBagConstraints.HORIZONTAL,
															new Insets(0, 0, 0,
																	0), 5, 0));

									final String name = c.getName();
									final float value = c.getPollData();

									final JLabel lblName = new JLabel();
									lblName.setPreferredSize(new Dimension(100,
											15));

									final GridBagConstraints nameGridBagConstraints = new GridBagConstraints(
											0, 0, 1, 1, 0.0, 0.0,
											GridBagConstraints.BASELINE,
											GridBagConstraints.NONE,
											new Insets(0, 0, 0, 0), 0, 0);

									final GridBagConstraints valueGridBagConstraints = new GridBagConstraints(
											2, 0, 1, 1, 1.0, 1.0,
											GridBagConstraints.BASELINE,
											GridBagConstraints.NONE,
											new Insets(0, 0, 0, 0), 0, 0);

									if (c.isAnalog()) {
										lblName.setText("Axis: " + name);
										panelComponent.add(lblName,
												nameGridBagConstraints);

										panelComponent.add(
												Box.createGlue(),
												new GridBagConstraints(
														1,
														GridBagConstraints.RELATIVE,
														1,
														1,
														1.0,
														1.0,
														GridBagConstraints.BASELINE,
														GridBagConstraints.NONE,
														new Insets(0, 0, 0, 0),
														0, 0));

										final JProgressBar progressBarValue = new JProgressBar(
												-100, 100);
										progressBarValue
												.setValue((int) (value * 100.0f));
										panelComponent.add(progressBarValue,
												valueGridBagConstraints);
									} else {
										lblName.setText("Button: " + name);
										panelComponent.add(lblName,
												nameGridBagConstraints);

										panelComponent.add(
												Box.createGlue(),
												new GridBagConstraints(
														1,
														GridBagConstraints.RELATIVE,
														1,
														1,
														1.0,
														1.0,
														GridBagConstraints.BASELINE,
														GridBagConstraints.NONE,
														new Insets(0, 0, 0, 0),
														0, 0));

										final JLabel lblValue = new JLabel();
										if (value > 0.5f)
											lblValue.setText("Down");
										else {
											lblValue.setText("Up");
											lblValue.setForeground(Color.LIGHT_GRAY);
										}
										panelComponent.add(lblValue,
												valueGridBagConstraints);
									}

									panelComponent.add(
											Box.createGlue(),
											new GridBagConstraints(
													3,
													GridBagConstraints.RELATIVE,
													1,
													1,
													1.0,
													1.0,
													GridBagConstraints.BASELINE,
													GridBagConstraints.NONE,
													new Insets(0, 0, 0, 0), 0,
													0));

									final JButton editButton = new JButton(
											new EditComponentAction(c));
									editButton
											.setPreferredSize(BUTTON_DIMENSION);
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
									panelComponent
											.add(editButton,
													new GridBagConstraints(
															4,
															GridBagConstraints.RELATIVE,
															1,
															1,
															0.0,
															0.0,
															GridBagConstraints.BASELINE,
															GridBagConstraints.NONE,
															new Insets(0, 0, 0,
																	0), 0, 0));
								}

								panelAssignments.add(
										Box.createGlue(),
										new GridBagConstraints(
												0,
												GridBagConstraints.RELATIVE,
												1,
												1,
												1.0,
												1.0,
												GridBagConstraints.FIRST_LINE_START,
												GridBagConstraints.NONE,
												new Insets(0, 0, 0, 0), 0, 0));
							} else
								panelAssignments.add(new JLabel(
										"No active controller selected!"));

							scrollPaneAssignments
									.setViewportView(panelAssignments);
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

	private class SetModeDescriptionAction extends AbstractAction implements
			FocusListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final Mode mode;
		private final JTextField modeDescriptionTextField;

		public SetModeDescriptionAction(Mode mode,
				JTextField modeDescriptionTextField) {
			this.mode = mode;
			this.modeDescriptionTextField = modeDescriptionTextField;

			putValue(NAME, "Set mode description");
			putValue(SHORT_DESCRIPTION, "Sets the description of a mode");
		}

		public void actionPerformed(ActionEvent e) {
			setModeDescription();
		}

		@Override
		public void focusGained(FocusEvent e) {
		}

		@Override
		public void focusLost(FocusEvent e) {
			setModeDescription();
		}

		private void setModeDescription() {
			final String description = modeDescriptionTextField.getText();

			if (description != null && description.length() > 0)
				mode.setDescription(description);
			else
				modeDescriptionTextField.setText(mode.getDescription());
		}
	}

	private class DeleteModeAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final Mode mode;

		public DeleteModeAction(Mode mode) {
			this.mode = mode;

			putValue(NAME, "Delete");
			putValue(SHORT_DESCRIPTION, "Delete the '" + mode.getDescription()
					+ "' mode");
		}

		public void actionPerformed(ActionEvent e) {
			Input.getProfile().getModes().remove(mode);
			updateModesPanel();
		}
	}

	private class AddModeAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public AddModeAction() {
			putValue(NAME, "Add");
			putValue(SHORT_DESCRIPTION, "Add a new mode");
		}

		public void actionPerformed(ActionEvent e) {
			final Mode mode = new Mode();
			Input.getProfile().getModes().add(mode);

			updateModesPanel();
		}
	}

	private class InterfaceAdapter<T> implements JsonSerializer<T>,
			JsonDeserializer<T> {
		public JsonElement serialize(T object,
				java.lang.reflect.Type interfaceType,
				JsonSerializationContext context) {
			final JsonObject wrapper = new JsonObject();
			wrapper.addProperty("type", object.getClass().getName());
			wrapper.add("data", context.serialize(object));
			return wrapper;
		}

		public T deserialize(JsonElement elem,
				java.lang.reflect.Type interfaceType,
				JsonDeserializationContext context) throws JsonParseException {
			final JsonObject wrapper = (JsonObject) elem;
			final JsonElement typeName = get(wrapper, "type");
			final JsonElement data = get(wrapper, "data");
			final java.lang.reflect.Type actualType = typeForName(typeName);
			return context.deserialize(data, actualType);
		}

		private java.lang.reflect.Type typeForName(final JsonElement typeElem) {
			try {
				return Class.forName(typeElem.getAsString());
			} catch (ClassNotFoundException e) {
				throw new JsonParseException(e);
			}
		}

		private JsonElement get(final JsonObject wrapper, String memberName) {
			final JsonElement elem = wrapper.get(memberName);
			if (elem == null)
				throw new JsonParseException(
						"no '"
								+ memberName
								+ "' member found in what was expected to be an interface wrapper");
			return elem;
		}
	}

}
