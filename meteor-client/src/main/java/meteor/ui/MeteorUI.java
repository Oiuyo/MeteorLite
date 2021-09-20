package meteor.ui;

import com.google.inject.Inject;
import com.google.inject.Provides;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import meteor.PluginManager;
import meteor.config.ConfigManager;
import meteor.eventbus.EventBus;
import meteor.eventbus.Subscribe;
import meteor.eventbus.events.ClientShutdown;
import meteor.eventbus.events.ConfigChanged;
import meteor.events.ExternalsReloaded;
import meteor.config.MeteorLiteConfig;
import meteor.ui.controllers.ToolbarController;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.events.GameTick;
import org.sponge.util.Logger;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;

import static meteor.MeteorLiteClientModule.parameters;
import static meteor.MeteorLiteClientModule.properties;

@Singleton
public class MeteorUI extends ContainableFrame implements AppletStub, AppletContext {
	private static final Logger log = new Logger("MeteorUI");
	private static final String CONFIG_CLIENT_BOUNDS = "clientBounds";
	private static final String CONFIG_CLIENT_MAXIMIZED = "clientMaximized";
	private static final String CONFIG_OPACITY = "enableOpacity";
	private static final String CONFIG_OPACITY_AMOUNT = "opacityPercentage";
	private static final int CLIENT_WELL_HIDDEN_MARGIN = 160;
	private static final int CLIENT_WELL_HIDDEN_MARGIN_TOP = 10;
	public static final int TOOLBAR_HEIGHT = 33;
	public static final int RIGHT_PANEL_WIDTH = 350;

	public static final int CLIENT_WIDTH = Constants.GAME_FIXED_WIDTH + (Constants.GAME_FIXED_WIDTH - 749);
	public static final int CLIENT_HEIGHT = Constants.GAME_FIXED_HEIGHT + (Constants.GAME_FIXED_HEIGHT - 464) + TOOLBAR_HEIGHT;
	public static final Dimension CLIENT_SIZE = new Dimension(CLIENT_WIDTH, CLIENT_HEIGHT);

	private final JPanel rootPanel = new JPanel();

	private Parent pluginsRoot;
	private Parent toolbarRoot;
	private Cursor defaultCursor;

	@Inject
	private Applet applet;

	@Inject
	private EventBus eventBus;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private MeteorLiteConfig config;

	@Inject
	private Client client;

	private Dimension lastClientSize;
	private final JFXPanel rightPanel = new JFXPanel();
	private Scene pluginsRootScene;
	public static boolean pluginsPanelVisible = false;

	public void init() throws IOException, InterruptedException, InvocationTargetException {
		applet.setMinimumSize(Constants.GAME_FIXED_SIZE);
		setAppletConfiguration(applet);

		//Early init game panel so gpu doesn't eat shit when enabling
		JPanel gamePanel = new JPanel();
		rootPanel.setLayout(new BorderLayout());
		gamePanel.setMinimumSize(Constants.GAME_FIXED_SIZE);
		try {
			toolbarRoot = FXMLLoader.load(
							Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("toolbar.fxml")));
		} catch (IOException e) {
			e.printStackTrace();
		}

		gamePanel.setLayout(new BorderLayout());
		gamePanel.add(applet, BorderLayout.CENTER);
		rootPanel.add(gamePanel, BorderLayout.CENTER);
		rootPanel.setMinimumSize(Constants.GAME_FIXED_SIZE);

		configManager.load();
		pluginManager.startInternalPlugins();

		// preload plugins scene, needs to be after pluginManager.startInternalPlugins() is called.
		try {
			pluginsRootScene = new Scene(pluginsRoot = FXMLLoader.load(
							Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("plugins.fxml"))), 350, 800);
		} catch (IOException e) {
			e.printStackTrace();
		}
		rightPanel.setScene(pluginsRootScene);

		setupJavaFXComponents(applet);

		updateFrameConfig(true);
		setWindowBounds();

		setVisible(true);
	}

	private void setWindowBounds() {
		if (config.rememberScreenBounds()) {
			try {
				Rectangle clientBounds = configManager.getConfiguration(
								MeteorLiteConfig.GROUP_NAME, CONFIG_CLIENT_BOUNDS, Rectangle.class);
				if (clientBounds != null) {
					revalidateMinimumSize();
					setLocation(clientBounds.getLocation());
					setSize(clientBounds.getSize());
				} else {
					setLocationRelativeTo(getOwner());
				}

				if (configManager.getConfiguration(MeteorLiteConfig.GROUP_NAME, CONFIG_CLIENT_MAXIMIZED) != null) {
					setExtendedState(JFrame.MAXIMIZED_BOTH);
				}
			} catch (Exception ex) {
				log.warn("Failed to set window bounds", ex);
				setLocationRelativeTo(getOwner());
			}
		} else {
			setLocationRelativeTo(getOwner());
		}

		// If the frame is well hidden (e.g. unplugged 2nd screen),
		// we want to move it back to default position as it can be
		// hard for the user to reposition it themselves otherwise.
		Rectangle clientBounds = getBounds();
		Rectangle screenBounds = getGraphicsConfiguration().getBounds();
		if (clientBounds.x + clientBounds.width - CLIENT_WELL_HIDDEN_MARGIN < screenBounds.getX() ||
						clientBounds.x + CLIENT_WELL_HIDDEN_MARGIN > screenBounds.getX() + screenBounds.getWidth() ||
						clientBounds.y + CLIENT_WELL_HIDDEN_MARGIN_TOP < screenBounds.getY() ||
						clientBounds.y + CLIENT_WELL_HIDDEN_MARGIN > screenBounds.getY() + screenBounds.getHeight()) {
			setLocationRelativeTo(getOwner());
		}
	}

	private void shutdownClient() {
		log.info("Shutting down client");
		saveClientBoundsConfig();
		ClientShutdown csev = new ClientShutdown();
		eventBus.post(csev);
		new Thread(() ->
		{
			if (applet != null) {
				int clientShutdownWaitMS;
				if (applet instanceof Client) {
					((Client) applet).stopNow();
					clientShutdownWaitMS = 1000;
				} else {
					applet.stop();
					setVisible(false);
					clientShutdownWaitMS = 6000;
				}

				try {
					Thread.sleep(clientShutdownWaitMS);
				} catch (InterruptedException ignored) {
				}
			}
			System.exit(0);
		}, "MeteorLite Shutdown").start();
	}

	private void saveClientBoundsConfig() {
		if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0) {
			configManager.setConfiguration(MeteorLiteConfig.GROUP_NAME, CONFIG_CLIENT_MAXIMIZED, true);
		} else {
			final Rectangle bounds = getBounds();
			configManager.unsetConfiguration(MeteorLiteConfig.GROUP_NAME, CONFIG_CLIENT_MAXIMIZED);
			configManager.setConfiguration(MeteorLiteConfig.GROUP_NAME, CONFIG_CLIENT_BOUNDS, bounds);
		}
	}

	private void updateFrameConfig(boolean updateResizable) {
		if (isAlwaysOnTopSupported()) {
			setAlwaysOnTop(config.gameAlwaysOnTop());
		}

		if (updateResizable) {
			setResizable(!config.lockWindowSize());
		}

		setExpandResizeType(config.automaticResizeType());

		ContainableFrame.Mode containMode = config.containInScreen();
		if (containMode == ContainableFrame.Mode.ALWAYS) {
			// When native window decorations are enabled we don't have a way to receive window move events
			// so we can't contain to screen always.
			containMode = ContainableFrame.Mode.RESIZING;
		}

		setContainedInScreen(containMode);

		if (!config.rememberScreenBounds()) {
			configManager.unsetConfiguration(MeteorLiteConfig.GROUP_NAME, CONFIG_CLIENT_MAXIMIZED);
			configManager.unsetConfiguration(MeteorLiteConfig.GROUP_NAME, CONFIG_CLIENT_BOUNDS);
		}

		if (applet == null) {
			return;
		}

		// The upper bounds are defined by the applet's max size
		// The lower bounds are defined by the client's fixed size
		int width = Math.max(Math.min(config.gameSize().width, 7680), Constants.GAME_FIXED_WIDTH);
		int height = Math.max(Math.min(config.gameSize().height, 2160), Constants.GAME_FIXED_HEIGHT);
		final Dimension size = new Dimension(width, height);

		if (!size.equals(lastClientSize)) {
			lastClientSize = size;
			applet.setSize(size);
			applet.setPreferredSize(size);
			applet.getParent().setPreferredSize(size);
			applet.getParent().setSize(size);

			if (isVisible()) {
				pack();
			}
		}
	}

	public void toggleRightPanel() throws IOException {
		if (pluginsPanelVisible) {
			rightPanel.setVisible(false);
		} else {
			rightPanel.setVisible(true);
		}

		pluginsPanelVisible = !pluginsPanelVisible;
		if (pluginsPanelVisible) {
			try {
				// if maximized, dont resize
				if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
					return;
				}

				// if panel would extend past screen, dont resize
				Dimension currentSize = getSize();
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				if (currentSize.getWidth() + RIGHT_PANEL_WIDTH > screenSize.getWidth()) {
					return;
				}

				// If resizing the game would go below the minimum size, always extend panel.
				if (getWidth() < CLIENT_WIDTH + RIGHT_PANEL_WIDTH) {
					setSize(new Dimension(getWidth() + RIGHT_PANEL_WIDTH, getHeight()));
					return;
				}

				if (config.resizeGame()) {
					return;
				}

				// if current client size is less than window size, but showing the panel would go past the screen, set size equal to screen size.
				Dimension newClientSize = new Dimension(getWidth() + RIGHT_PANEL_WIDTH, getHeight());
				if (newClientSize.getWidth() > screenSize.getWidth()) {
					newClientSize = screenSize;
					setExtendedState(JFrame.MAXIMIZED_BOTH);
				}
				setSize(newClientSize);
			} finally {
				setMinimumSize(new Dimension(CLIENT_WIDTH + RIGHT_PANEL_WIDTH, CLIENT_HEIGHT));
				validate();
			}
		} else {
			setMinimumFrameSize();
		}
	}

	private void setMinimumFrameSize() {
		// if resize game is checked, and we are at the minimum size, still resize.
		boolean resize = getMinimumSize().equals(getSize());
		setMinimumSize(CLIENT_SIZE);

		// if maximized, dont resize
		if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
			return;
		}

		// if size is the same as screen size, dont resize
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (getSize().equals(screenSize)) {
			return;
		}

		if (!config.resizeGame()) {
			setSize(new Dimension(getWidth() - RIGHT_PANEL_WIDTH, getHeight()));
		}
		if (resize) {
			setSize(getMinimumSize());
		}
		validate();
	}

	public void showPlugins() {
		rightPanel.getScene().setRoot(pluginsRoot);
	}

	public void updateRightPanel(Parent root, int width) {
		rightPanel.getScene().setRoot(root);
	}

	public void setupJavaFXComponents(Applet applet) {
		setMinimumFrameSize();
		JFXPanel toolbarPanel = new JFXPanel();
		toolbarPanel.setSize(1280, 100);
		rightPanel.setSize(550, 800);

		toolbarPanel.setScene(new Scene(toolbarRoot, 300, 33));
		toolbarPanel.setVisible(true);
		rightPanel.setVisible(false);

		rootPanel.add(toolbarPanel, BorderLayout.NORTH);
		rootPanel.add(rightPanel, BorderLayout.EAST);
		add(rootPanel);
		rootPanel.setVisible(true);

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				shutdownClient();
			}
		});
		applet.setSize(Constants.GAME_FIXED_WIDTH, Constants.GAME_FIXED_HEIGHT);
		applet.init();
		applet.start();
	}

	private Dimension appletMinSize() {
		return new Dimension(
						Integer.parseInt(properties.get("applet_minwidth")),
						Integer.parseInt(properties.get("applet_minheight"))
		);
	}

	private Dimension appletMaxSize() {
		return new Dimension(
						Integer.parseInt(properties.get("applet_maxwidth")),
						Integer.parseInt(properties.get("applet_maxheight"))
		);
	}

	public Applet setAppletConfiguration(Applet applet) {
		applet.setStub(this);
		applet.setMaximumSize(appletMaxSize());
		applet.setMinimumSize(appletMinSize());
		applet.setPreferredSize(applet.getMinimumSize());
		return applet;
	}

	public void setCursor(final BufferedImage image, final String name) {
		final java.awt.Point hotspot = new java.awt.Point(0, 0);
		final Cursor cursorAwt = Toolkit.getDefaultToolkit().createCustomCursor(image, hotspot, name);
		defaultCursor = cursorAwt;
		setCursor(cursorAwt);
	}

	public Cursor getCurrentCursor() {
		return getCursor();
	}

	public Cursor getDefaultCursor() {
		return defaultCursor != null ? defaultCursor : Cursor.getDefaultCursor();
	}

	public void setCursor(final Cursor cursor) {
		super.setCursor(cursor);
	}

	public void resetCursor() {
		defaultCursor = null;
		super.setCursor(Cursor.getDefaultCursor());
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals(MeteorLiteConfig.GROUP_NAME)
						&& !(event.getKey().equals(CONFIG_OPACITY) ||
						event.getKey().equals(CONFIG_OPACITY_AMOUNT)) ||
						event.getKey().equals(CONFIG_CLIENT_MAXIMIZED) ||
						event.getKey().equals(CONFIG_CLIENT_BOUNDS)) {
			return;
		}

		SwingUtilities.invokeLater(() -> updateFrameConfig(event.getKey().equals("lockWindowSize")));
	}

	@Subscribe
	public void onExternalsReloaded(ExternalsReloaded e) {
		pluginManager.startExternals();
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		//This fixes bad drawing
		if (client.getGameDrawingMode() != 2) {
			client.setGameDrawingMode(2);
		}

		if (client.getLocalPlayer().isIdle())
			ToolbarController.idleButtonInstance.setVisible(true);
		else
			ToolbarController.idleButtonInstance.setVisible(false);
	}

	@Provides
	@Named("rightPanelScene")
	public Scene getRightPanel() {
		return pluginsRootScene;
	}

	@Override
	public URL getCodeBase() {
		try {
			return new URL(properties.get("codebase"));
		} catch (MalformedURLException e) {
			throw new InvalidParameterException();
		}
	}

	@Override
	public URL getDocumentBase() {
		return getCodeBase();
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public void showDocument(URL url) {
		try {
			Desktop.getDesktop().browse(url.toURI());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void showDocument(URL url, String target) {
		showDocument(url);
	}

	@Override
	public AppletContext getAppletContext() {
		return this;
	}

	@Override
	public void appletResize(int width, int height) {
	}

	@Override
	public AudioClip getAudioClip(URL url) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Image getImage(URL url) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Applet getApplet(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Enumeration<Applet> getApplets() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void showStatus(String status) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setStream(String key, InputStream stream) {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getStream(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<String> getStreamKeys() {
		throw new UnsupportedOperationException();
	}
}
