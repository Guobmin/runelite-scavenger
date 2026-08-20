package com.scavenger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

class ScavengerPanel extends PluginPanel
{
	private final TargetManager targetManager;
	private final DefaultListModel<ItemSpawn> listModel = new DefaultListModel<>();
	private final Timer refreshTimer;

	private final JTextArea targetName = cardLine(FontManager.getRunescapeBoldFont());
	private final JTextArea targetLocation = cardLine(FontManager.getRunescapeFont());
	private final JTextArea targetFloor = cardLine(FontManager.getRunescapeFont());
	private final JTextArea requirementText = cardLine(FontManager.getRunescapeFont());

	@Inject
	ScavengerPanel(TargetManager targetManager)
	{
		// unwrapped: lets us own our own JScrollPane and stretch it to fill
		// the sidebar's full height, instead of RuneLite's default NORTH-pinned
		// wrapper that sizes to content only (matches Quest Helper's approach).
		super(false);
		this.targetManager = targetManager;

		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		IconTextField searchField = new IconTextField();
		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchField.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		// IconTextField doesn't expose its internal JTextField's foreground, so
		// walk the (public) component tree to fix the unreadable default text
		// color instead of leaving it at LAF-default black-on-dark.
		forceTextColor(searchField, Color.WHITE);
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				onSearchChanged(searchField.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				onSearchChanged(searchField.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				onSearchChanged(searchField.getText());
			}
		});

		JList<ItemSpawn> resultList = new JList<>(listModel);
		resultList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		resultList.setCellRenderer(new ItemRenderer());
		resultList.addListSelectionListener((ListSelectionListener) e ->
		{
			if (!e.getValueIsAdjusting() && resultList.getSelectedValue() != null)
			{
				targetManager.setActiveItem(resultList.getSelectedValue());
			}
		});

		JButton stopButton = new JButton("Stop Tracking");
		stopButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stopButton.setForeground(ColorScheme.TEXT_COLOR);
		stopButton.setFocusPainted(false);
		stopButton.addActionListener(e ->
		{
			targetManager.clearActiveItem();
			resultList.clearSelection();
		});

		JLabel searchHeader = sectionHeader("Item Search");

		JPanel searchPanel = new JPanel(new BorderLayout(0, 2));
		searchPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		searchPanel.add(searchHeader, BorderLayout.NORTH);
		searchPanel.add(searchField, BorderLayout.CENTER);

		JScrollPane resultScroll = new JScrollPane(resultList);
		resultScroll.setBorder(null);
		resultScroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		// Long item names were triggering an unthemed horizontal scrollbar;
		// clip instead of scrolling sideways, matching PluginPanel's own
		// wrapped scroll pane and Quest Helper's result list.
		resultScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		resultScroll.getVerticalScrollBar().setUnitIncrement(16);

		JPanel top = new JPanel(new BorderLayout(0, 4));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);
		top.add(searchPanel, BorderLayout.NORTH);
		top.add(resultScroll, BorderLayout.CENTER);

		targetName.setForeground(Color.WHITE);
		targetLocation.setForeground(ColorScheme.TEXT_COLOR);
		targetFloor.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel targetPanel = card("Target", targetName, targetLocation, targetFloor);

		requirementText.setForeground(ColorScheme.TEXT_COLOR);
		JPanel requirementsPanel = card("Requirements", requirementText);

		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
		bottom.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bottom.add(targetPanel);
		bottom.add(Box.createVerticalStrut(8));
		bottom.add(requirementsPanel);
		bottom.add(Box.createVerticalStrut(8));
		bottom.add(stopButton);

		add(top, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);

		// ponytail: polling instead of an event subscription — TargetManager's
		// result is recomputed on the client thread every GameTick and this is
		// the simplest way for a Swing component to observe it without wiring
		// its own EventBus subscription just for label text.
		refreshTimer = new Timer(500, e -> updateActiveLabel());
		refreshTimer.start();

		updateActiveLabel();
		onSearchChanged("");
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		// RuneLite starts plugins (building this panel) before it installs
		// RuneLiteLAF, so descendants like the scrollbar resolve their UI
		// delegate against Swing's Metal default at construction time and
		// never see the real theme. addNotify() only fires once this panel
		// is actually shown, which is guaranteed to be after LAF setup —
		// refresh the whole tree against whatever LAF is active by then.
		SwingUtilities.updateComponentTreeUI(this);
	}

	private static JLabel sectionHeader(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.TEXT_COLOR);
		label.setBorder(new EmptyBorder(0, 0, 2, 0));
		return label;
	}

	private static JPanel card(String title, JComponent... lines)
	{
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		body.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
			new EmptyBorder(8, 8, 8, 8)));
		for (JComponent line : lines)
		{
			line.setAlignmentX(Component.LEFT_ALIGNMENT);
			body.add(line);
		}

		JPanel wrapper = new JPanel(new BorderLayout(0, 2));
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(sectionHeader(title), BorderLayout.NORTH);
		wrapper.add(body, BorderLayout.CENTER);
		return wrapper;
	}

	// non-editable, word-wrapping line for use inside a card — JTextArea wraps
	// reliably based on its assigned layout width, unlike JLabel's HTML-CSS
	// width trick which BasicHTML doesn't always honor under BoxLayout.
	private static JTextArea cardLine(Font font)
	{
		JTextArea area = new JTextArea();
		area.setEditable(false);
		area.setFocusable(false);
		area.setOpaque(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setFont(font);
		area.setForeground(ColorScheme.TEXT_COLOR);
		area.setBorder(null);
		return area;
	}

	private static void forceTextColor(Container root, Color color)
	{
		for (Component c : root.getComponents())
		{
			if (c instanceof JTextField)
			{
				((JTextField) c).setForeground(color);
				((JTextField) c).setCaretColor(color);
			}
			if (c instanceof Container)
			{
				forceTextColor((Container) c, color);
			}
		}
	}

	private void onSearchChanged(String query)
	{
		// Defer the rebuild: clearing/repopulating the list model synchronously
		// inside insertUpdate blocks the EDT and desyncs the text field's caret.
		SwingUtilities.invokeLater(() ->
		{
			listModel.clear();
			for (ItemSpawn item : targetManager.search(query))
			{
				listModel.addElement(item);
			}
		});
	}

	private void updateActiveLabel()
	{
		ItemSpawn active = targetManager.getActiveItem();
		if (active == null)
		{
			targetName.setText("");
			targetLocation.setText("");
			targetFloor.setText("");
			requirementText.setForeground(ColorScheme.TEXT_COLOR);
			requirementText.setText("");
			return;
		}

		targetName.setText(active.name);

		NearestLocationFinder.Result result = targetManager.getActiveResult();
		if (result == null)
		{
			targetLocation.setText("");
			targetFloor.setText("");
			requirementText.setForeground(ColorScheme.TEXT_COLOR);
			requirementText.setText("");
			return;
		}

		targetLocation.setText("(" + result.location.areaLabel + ")");
		targetFloor.setText(result.samePlane ? "" : "- different floor");

		if (result.location.requirement == null)
		{
			requirementText.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
			requirementText.setText("No requirements needed.");
		}
		else
		{
			requirementText.setForeground(ColorScheme.BRAND_ORANGE);
			requirementText.setText(result.location.requirement);
		}
	}

	void shutdown()
	{
		refreshTimer.stop();
	}

	private static class ItemRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
			boolean isSelected, boolean cellHasFocus)
		{
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			label.setOpaque(true);
			label.setBorder(new EmptyBorder(5, 8, 5, 8));
			label.setBackground(isSelected ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			label.setForeground(isSelected ? Color.WHITE : ColorScheme.TEXT_COLOR);
			return label;
		}
	}
}
