package de.bwravencl.RemoteStick.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.KeyStroke;
import de.bwravencl.RemoteStick.input.Profile;
import de.bwravencl.RemoteStick.input.action.ButtonToProfileAction;
import de.bwravencl.RemoteStick.input.action.CursorAction.Axis;
import de.bwravencl.RemoteStick.input.action.IAction;
import net.java.games.input.Component;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EditComponentDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String ACTION_CLASS_PREFIX = "de.bwravencl.RemoteStick.input.action.";
	public static final String[] AXIS_ACTION_CLASSES = {
			ACTION_CLASS_PREFIX + "AxisToAxisAction",
			ACTION_CLASS_PREFIX + "AxisToButtonAction",
			ACTION_CLASS_PREFIX + "AxisToKeyAction",
			ACTION_CLASS_PREFIX + "AxisToRelativeAxisAction",
			ACTION_CLASS_PREFIX + "AxisToScrollAction" };
	public static final String[] BUTTON_ACTION_CLASSES = {
			ACTION_CLASS_PREFIX + "ButtonToButtonAction",
			ACTION_CLASS_PREFIX + "ButtonToKeyAction",
			ACTION_CLASS_PREFIX + "ButtonToProfileAction",
			ACTION_CLASS_PREFIX + "ButtonToScrollAction" };

	public static final String ACTION_PROPERTY_GETTER_PREFIX_DEFAULT = "get";
	public static final String ACTION_PROPERTY_GETTER_PREFIX_BOOLEAN = "is";
	public static final String ACTION_PROPERTY_SETTER_PREFIX = "set";

	private final JComboBox<Profile> comboBoxProfile;
	private final JList<AvailableAction> listAvailableActions = new JList<AvailableAction>();
	private final JButton btnAdd;
	private final JButton btnRemove;
	private final JList<IAction> listAssignedActions = new JList<IAction>();
	private final JLabel lblProperties;
	private final JScrollPane scrollPaneProperties;

	private final Input input;
	private final Component component;
	private final Map<String, ButtonToProfileAction> unsavedComponentToProfileActionMap;
	private final List<Profile> unsavedProfiles;
	private Profile selectedProfile;
	private AvailableAction selectedAvailableAction;
	private IAction selectedAssignedAction;

	/**
	 * Create the dialog.
	 */
	public EditComponentDialog(Input input, Component component) {
		this.input = input;
		this.component = component;

		unsavedComponentToProfileActionMap = new HashMap<String, ButtonToProfileAction>();
		for (Map.Entry<String, ButtonToProfileAction> e : input
				.getComponentToProfileActionMap().entrySet())
			try {
				unsavedComponentToProfileActionMap.put(new String(e.getKey()),
						(ButtonToProfileAction) e.getValue().clone());
			} catch (CloneNotSupportedException e1) {
				e1.printStackTrace();
			}

		unsavedProfiles = new ArrayList<Profile>();
		for (Profile p : input.getProfiles())
			try {
				unsavedProfiles.add((Profile) p.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}

		setModal(true);
		setTitle("Component Editor '" + component.getName() + "'");
		setBounds(100, 100, 800, 400);
		getContentPane().setLayout(new BorderLayout());

		final JPanel panelProfile = new JPanel(new FlowLayout());
		getContentPane().add(panelProfile, BorderLayout.NORTH);

		panelProfile.add(new JLabel("Profile"));

		selectedProfile = unsavedProfiles.get(0);
		comboBoxProfile = new JComboBox<Profile>(
				unsavedProfiles.toArray(new Profile[unsavedProfiles.size()]));
		comboBoxProfile.addActionListener(new SelectProfileAction());
		panelProfile.add(comboBoxProfile);

		final JPanel panelActions = new JPanel(new GridBagLayout());
		panelActions.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panelActions, BorderLayout.CENTER);

		panelActions.add(new JLabel("Available Actions"),
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 25));

		listAvailableActions
				.addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						selectedAvailableAction = listAvailableActions
								.getSelectedValue();
						if (selectedAvailableAction == null)
							btnAdd.setEnabled(false);
						else
							btnAdd.setEnabled(true);
					}
				});
		updateAvailableActions();
		panelActions.add(new JScrollPane(listAvailableActions),
				new GridBagConstraints(0, 1, 1, 5, 0.33, 1.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));

		btnAdd = new JButton(new AddActionAction());
		btnAdd.setEnabled(false);
		panelActions.add(btnAdd, new GridBagConstraints(1, 2, 1, 2, 0.0, 0.25,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
						0, 0, 0, 0), 0, 0));

		btnRemove = new JButton(new RemoveActionAction());
		btnRemove.setEnabled(false);
		panelActions.add(btnRemove, new GridBagConstraints(1, 4, 1, 2, 0.0,
				0.25, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));

		panelActions.add(new JLabel("Assigned Actions"),
				new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 25));

		listAssignedActions
				.addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						selectedAssignedAction = listAssignedActions
								.getSelectedValue();
						if (selectedAssignedAction == null)
							btnRemove.setEnabled(false);
						else
							btnRemove.setEnabled(true);

						EventQueue.invokeLater(new Runnable() {

							@Override
							public void run() {
								if (selectedAssignedAction == null) {
									lblProperties.setVisible(false);
									scrollPaneProperties.setVisible(false);
								} else {
									lblProperties.setVisible(true);

									final JPanel panelProperties = new JPanel(
											new GridBagLayout());

									for (Method m : selectedAssignedAction
											.getClass().getMethods()) {
										final String methodDescription = m
												.toGenericString();

										if (methodDescription
												.contains(ACTION_PROPERTY_SETTER_PREFIX)) {
											final String propertyName = methodDescription.substring(
													methodDescription
															.indexOf(ACTION_PROPERTY_SETTER_PREFIX)
															+ ACTION_PROPERTY_SETTER_PREFIX
																	.length(),
													methodDescription
															.indexOf('('));
											final String parameterType = methodDescription.substring(
													methodDescription
															.indexOf('(') + 1,
													methodDescription
															.indexOf(')'));

											final Class<?> clazz;
											try {
												clazz = Class
														.forName(parameterType);

												final Method getterMethod = selectedAssignedAction
														.getClass()
														.getMethod(
																(clazz == Boolean.class ? ACTION_PROPERTY_GETTER_PREFIX_BOOLEAN
																		: ACTION_PROPERTY_GETTER_PREFIX_DEFAULT)
																		+ propertyName);

												final JPanel panelProperty = new JPanel(
														new FlowLayout(
																FlowLayout.LEADING,
																10, 0));
												panelProperties
														.add(panelProperty,
																new GridBagConstraints(
																		0,
																		GridBagConstraints.RELATIVE,
																		1,
																		1,
																		0.0,
																		0.0,
																		GridBagConstraints.FIRST_LINE_START,
																		GridBagConstraints.NONE,
																		new Insets(
																				0,
																				0,
																				0,
																				0),
																		0, 10));

												final JLabel lblPropertyName = new JLabel(
														propertyName);
												lblPropertyName
														.setPreferredSize(new Dimension(
																100, 15));
												panelProperty
														.add(lblPropertyName);

												if (Boolean.class == clazz) {
													final JCheckBox checkBox = new JCheckBox(
															new JCheckBoxSetPropertyAction(
																	m));
													checkBox.setSelected((boolean) getterMethod
															.invoke(selectedAssignedAction));
													panelProperty.add(checkBox);
												} else if (Integer.class == clazz) {
													final JSpinner spinner = new JSpinner();
													spinner.addChangeListener(new JSpinnerSetPropertyChangeListener(
															m));
													spinner.setValue(getterMethod
															.invoke(selectedAssignedAction));
													panelProperty.add(spinner);
												} else if (Float.class == clazz) {
													final JSpinner spinner = new JSpinner();
													spinner.addChangeListener(new JSpinnerSetPropertyChangeListener(
															m));
													spinner.setValue(getterMethod
															.invoke(selectedAssignedAction));
													panelProperty.add(spinner);
												} else if (UUID.class == clazz) {
													panelProperty
															.add(new JComboBox<>());
												} else if (Axis.class == clazz) {
													panelProperty
															.add(new JComboBox<>());
												} else if (KeyStroke.class == clazz) {
													panelProperty
															.add(new JComboBox<>());
												} else {
													System.out.println("Error: "
															+ clazz.getName()
															+ " GUI element not implemented!");
												}
											} catch (ClassNotFoundException e) {
												e.printStackTrace();
											} catch (IllegalAccessException e) {
												e.printStackTrace();
											} catch (IllegalArgumentException e) {
												e.printStackTrace();
											} catch (InvocationTargetException e) {
												e.printStackTrace();
											} catch (NoSuchMethodException e) {
												e.printStackTrace();
											} catch (SecurityException e) {
												e.printStackTrace();
											}
										}
									}

									panelProperties.add(
											Box.createGlue(),
											new GridBagConstraints(
													0,
													GridBagConstraints.RELATIVE,
													1, 1, 1.0, 1.0,
													GridBagConstraints.CENTER,
													GridBagConstraints.NONE,
													new Insets(0, 0, 0, 0), 0,
													0));

									scrollPaneProperties
											.setViewportView(panelProperties);
									scrollPaneProperties.setVisible(true);
								}
							}
						});
					}
				});
		panelActions.add(new JScrollPane(listAssignedActions),
				new GridBagConstraints(2, 1, 1, 5, 0.33, 1.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));

		lblProperties = new JLabel("Properties");
		lblProperties.setVisible(false);
		panelActions.add(lblProperties, new GridBagConstraints(3, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 25));

		scrollPaneProperties = new JScrollPane();
		scrollPaneProperties.setVisible(false);
		panelActions.add(scrollPaneProperties, new GridBagConstraints(3, 1, 1,
				5, 0.33, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		final JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		final JButton btnOK = new JButton(new OKAction());
		btnOK.setActionCommand("OK");
		buttonPane.add(btnOK);
		getRootPane().setDefaultButton(btnOK);

		final JButton btnCancel = new JButton(new CancelAction());
		btnCancel.setActionCommand("Cancel");
		buttonPane.add(btnCancel);

		updateAssignedActions();
	}

	private void updateAvailableActions() {
		boolean hasProfileAction = false;
		for (IAction a : getAssignedActions())
			if (a instanceof ButtonToProfileAction)
				hasProfileAction = true;

		final List<AvailableAction> availableActions = new ArrayList<AvailableAction>();

		for (String s : component.isAnalog() ? AXIS_ACTION_CLASSES
				: BUTTON_ACTION_CLASSES) {
			final AvailableAction availableAction = new AvailableAction(s);
			if (!hasProfileAction
					|| (hasProfileAction && !ButtonToProfileAction.class
							.getName().equals(availableAction.className)))
				availableActions.add(availableAction);
		}

		listAvailableActions.setListData(availableActions
				.toArray(new AvailableAction[availableActions.size()]));
	}

	private IAction[] getAssignedActions() {
		List<IAction> assignedActions = selectedProfile
				.getComponentToActionMap().get(component.getName());
		if (assignedActions == null)
			assignedActions = new ArrayList<IAction>();

		final ButtonToProfileAction buttonToProfileAction = unsavedComponentToProfileActionMap
				.get(component.getName());
		if (buttonToProfileAction != null)
			assignedActions.add(buttonToProfileAction);

		return (IAction[]) assignedActions.toArray(new IAction[assignedActions
				.size()]);
	}

	private void updateAssignedActions() {
		listAssignedActions.setListData(getAssignedActions());
	}

	void closeDialog() {
		setVisible(false);
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private class SelectProfileAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public SelectProfileAction() {
			putValue(NAME, "Select Profile");
			putValue(SHORT_DESCRIPTION, "Selects the profile to edit");
		}

		public void actionPerformed(ActionEvent e) {
			selectedProfile = (Profile) comboBoxProfile.getSelectedItem();
			updateAssignedActions();
		}
	}

	private class AddActionAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public AddActionAction() {
			putValue(NAME, "Add");
			putValue(SHORT_DESCRIPTION,
					"Add the selected action to the component");
		}

		public void actionPerformed(ActionEvent e) {
			try {
				final Class<?> clazz = Class
						.forName(selectedAvailableAction.className);
				final IAction action = (IAction) clazz.newInstance();

				if (action instanceof ButtonToProfileAction)
					unsavedComponentToProfileActionMap.put(component.getName(),
							(ButtonToProfileAction) action);
				else {
					final Map<String, List<IAction>> componentToActionMap = selectedProfile
							.getComponentToActionMap();
					final String componentName = component.getName();

					if (componentToActionMap.get(componentName) == null)
						componentToActionMap.put(componentName,
								new ArrayList<IAction>());

					componentToActionMap.get(componentName).add(action);
				}

				updateAvailableActions();
				updateAssignedActions();

				listAssignedActions.setSelectedIndex(listAssignedActions
						.getLastVisibleIndex());
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			}

		}
	}

	private class RemoveActionAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public RemoveActionAction() {
			putValue(NAME, "Remove");
			putValue(SHORT_DESCRIPTION,
					"Remove the selected action to the component");
		}

		public void actionPerformed(ActionEvent e) {
			if (selectedAssignedAction instanceof ButtonToProfileAction)
				unsavedComponentToProfileActionMap.remove(component.getName());
			else {
				final Map<String, List<IAction>> componentToActionMap = selectedProfile
						.getComponentToActionMap();
				final List<IAction> actions = componentToActionMap
						.get(component.getName());
				actions.remove(selectedAssignedAction);

				if (actions.size() == 0)
					componentToActionMap.remove(component.getName());
			}

			updateAvailableActions();
			updateAssignedActions();
		}
	}

	private class JCheckBoxSetPropertyAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final Method setterMethod;

		public JCheckBoxSetPropertyAction(Method setterMethod) {
			this.setterMethod = setterMethod;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				setterMethod.invoke(selectedAssignedAction,
						((JCheckBox) e.getSource()).isSelected());
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}
	}

	private class JSpinnerSetPropertyChangeListener implements ChangeListener {

		private final Method setterMethod;

		public JSpinnerSetPropertyChangeListener(Method setterMethod) {
			this.setterMethod = setterMethod;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			try {
				setterMethod.invoke(selectedAssignedAction,
						((JSpinner) e.getSource()).getValue());
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}
	}

	private class OKAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public OKAction() {
			putValue(NAME, "OK");
			putValue(SHORT_DESCRIPTION, "Apply changes");
		}

		public void actionPerformed(ActionEvent e) {
			input.setComponentToProfileActionMap(unsavedComponentToProfileActionMap);
			input.setProfiles(unsavedProfiles);

			closeDialog();
		}
	}

	private class CancelAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public CancelAction() {
			putValue(NAME, "Cancel");
			putValue(SHORT_DESCRIPTION, "Dismiss changes");
		}

		public void actionPerformed(ActionEvent e) {
			closeDialog();
		}
	}

	private class AvailableAction {

		private final String className;

		public AvailableAction(String className) {
			this.className = className;
		}

		@Override
		public String toString() {
			String description = null;

			try {
				final Class<?> clazz = Class.forName(className);
				description = clazz.newInstance().toString();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			return description;
		}

	}

}
