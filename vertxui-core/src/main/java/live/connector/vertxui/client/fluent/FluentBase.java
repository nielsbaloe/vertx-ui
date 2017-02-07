package live.connector.vertxui.client.fluent;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.gwt.core.client.GWT;

import elemental.dom.Document;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.KeyboardEvent;
import elemental.events.MouseEvent;
import elemental.events.UIEvent;
import elemental.html.Console;
import elemental.html.InputElement;
import elemental.html.SelectElement;
import elemental.html.Window;
import elemental.js.dom.JsDocument;
import elemental.js.html.JsWindow;
import live.connector.vertxui.client.test.ConsoleTester;

public class FluentBase implements Viewable {

	static {
		if (!GWT.isClient()) {
			document = null;
			window = null;
		} else {
			document = getDocument();// Browser.getDocument();
			window = getWindow(); // Browser.getWindow();
		}
	}
	public static Document document;
	public final static Window window;
	public final static Console console;
	public static Fluent body;
	public final static Fluent head;
	static {
		if (!GWT.isClient()) {
			console = new ConsoleTester();
			body = new Fluent(null);
			head = new Fluent(null);
		} else {
			console = window.getConsole();
			body = new Fluent(document.getBody());
			head = new Fluent(document.getHead());
		}
	}

	private static native JsWindow getWindow() /*-{
												return window.top;
												}-*/;

	private static native JsDocument getDocument() /*-{
													return window.top.document;
														}-*/;

	/**
	 * If we are attached to DOM , 'element' exists, otherwise this is null.
	 */
	protected Node element;
	protected Fluent parent;

	/**
	 * Attached or detached: these are tag, attrs and children. If tag null, we
	 * are a non-API tag.
	 */
	protected String tag;
	protected TreeMap<Att, String> attrs;
	protected TreeMap<Css, String> styles;
	protected TreeMap<String, EventListener> listeners;
	protected List<Viewable> childs;
	protected String text;

	/**
	 * API call for normal HTML elements. Without a parent: detached.
	 */
	protected FluentBase(String tag, Fluent parent) {
		this.tag = tag;
		this.parent = parent;

		// Update parent
		if (parent != null) {
			if (parent.childs == null) {
				parent.childs = new ArrayList<>();
			}
			parent.childs.add(this);
		}

		// Add to DOM if connected
		if (tag != null && parent != null && parent.element != null) {

			if (tag.equals("TEXT")) {
				element = document.createTextNode("");
			} else {
				element = document.createElement(tag);
			}
			if (parent != null) {
				// console.log("appending " + tag + " to parentTag: " +
				// parent.tag);
				parent.element.appendChild(element);
			}
		}
	}

	/**
	 * For when this represents an existing object: getBody() getHead() and
	 * dom().
	 * 
	 * @param parent
	 *            the existing object
	 */
	protected FluentBase(Node parent) {
		element = parent;
	}

	/**
	 * Do not create but GET the dom object.
	 */
	public Node dom() {
		return element;
	}

	/**
	 * Create a Fluent object by starting out with an existing node with an id,
	 * usefull for existing HTML pages.
	 * 
	 * @return a Fluent object with the give id, or null.
	 */
	public static Fluent dom(String id) {
		Node node = document.getElementById(id);
		if (node == null) {
			return null;
		}
		return new Fluent(node);
	}

	/**
	 * Set the textContent (HTML) for this element, avoiding the less stable
	 * innerHtml and innerTxt. Set to null (or empty string) to clear.
	 * 
	 * Note that it will also set the text of all children, so prevent combining
	 * text and children together. If you really really mean to have children
	 * and text together (which is opiniated bad practice), use text() to create
	 * a text node.
	 * 
	 */
	public Fluent txt(String text) {
		if (Renderer.equalsString(this.text, text)) {
			// console.log("Skipping, still " + text);
			return (Fluent) this;
		}
		this.text = text;
		if (element != null) {
			// console.log("setting textto "+text);
			element.setTextContent(text);
		}
		return (Fluent) this;
	}

	/**
	 * Gives the text that has been set; note that the real DOM text is "" if
	 * after you set it to null.
	 */
	public String txt() {
		return this.text;
	}

	/**
	 * Add or remove (by value null) an eventlistener.
	 * 
	 */
	public Fluent listen(String name, EventListener value) {
		if (listeners == null) {
			listeners = new TreeMap<>();
		}
		if (value != null) {
			listeners.put(name, value);
			if (element != null) {
				element.addEventListener(name, value);
			}
		} else { // remove
			EventListener oldValue = listeners.get(name);
			listeners.remove(name);
			if (element != null) {
				element.removeEventListener(name, oldValue);
			}
		}
		return (Fluent) this;
	}

	public EventListener listen(String event) {
		if (listeners == null) {
			return null;
		}
		return listeners.get(event);
	}

	public Fluent keyup(Consumer<KeyboardEvent> listener) {
		return listen(Event.KEYUP, evt -> {
			evt.stopPropagation();
			listener.accept((KeyboardEvent) evt);
		});
	}

	public Fluent click(Consumer<MouseEvent> listener) {
		return listen(Event.CLICK, evt -> {
			if (evt.getTarget() instanceof InputElement) {
				InputElement input = (InputElement) evt.getTarget();
				if (input.getAttribute("type") == "checkbox") {
					att(Att.checked, input.getAttribute("checked"));
				}
			}
			evt.stopPropagation();
			listener.accept((MouseEvent) evt);
		});
	}

	public Fluent load(EventListener listener) {
		return listen("LOAD", listener);
	}

	public Fluent focus(EventListener listener) {
		return listen(Event.FOCUS, listener);
	}

	public Fluent blur(Consumer<UIEvent> listener) {
		return listen(Event.BLUR, evt -> {
			evt.stopPropagation();
			listener.accept((UIEvent) evt);
		});
	}

	public Fluent keydown(Consumer<KeyboardEvent> listener) {
		return listen(Event.KEYDOWN, evt -> {
			evt.stopPropagation();
			listener.accept((KeyboardEvent) evt);
		});
	}

	public Fluent keypress(Consumer<KeyboardEvent> listener) {
		return listen(Event.KEYPRESS, evt -> {
			evt.stopPropagation();
			listener.accept((KeyboardEvent) evt);
		});
	}

	public Fluent dblclick(Consumer<MouseEvent> listener) {
		return listen(Event.DBLCLICK, evt -> {
			evt.stopPropagation();
			listener.accept((MouseEvent) evt);
		});
	}

	public Fluent mousedown(Consumer<MouseEvent> listener) {
		return listen(Event.MOUSEDOWN, evt -> {
			evt.stopPropagation();
			listener.accept((MouseEvent) evt);
		});
	}

	public Fluent mouseup(Consumer<MouseEvent> listener) {
		return listen(Event.MOUSEUP, evt -> {
			evt.stopPropagation();
			listener.accept((MouseEvent) evt);
		});
	}

	public Fluent mouseover(Consumer<MouseEvent> listener) {
		return listen(Event.MOUSEOVER, evt -> {
			evt.stopPropagation();
			listener.accept((MouseEvent) evt);
		});
	}

	public Fluent mouseenter(Consumer<MouseEvent> listener) {
		return listen("mouseenter", evt -> {
			evt.stopPropagation();
			listener.accept((MouseEvent) evt);
		});
	}

	public Fluent mouseleave(Consumer<MouseEvent> listener) {
		return listen("mouseleave", evt -> {
			evt.stopPropagation();
			listener.accept((MouseEvent) evt);
		});
	}

	public Fluent mousemove(Consumer<MouseEvent> listener) {
		return listen(Event.MOUSEMOVE, evt -> {
			evt.stopPropagation();
			listener.accept((MouseEvent) evt);
		});
	}

	public Fluent mouseout(Consumer<MouseEvent> listener) {
		return listen(Event.MOUSEOUT, evt -> {
			evt.stopPropagation();
			listener.accept((MouseEvent) evt);
		});
	}

	public Fluent css(Css name, String value, Css name2, String value2) {
		css(name, value);
		css(name2, value2);
		return (Fluent) this;
	}

	/**
	 * Set or remove (by value null) a css style.
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public Fluent css(Css name, String value) {
		if (styles == null) {
			styles = new TreeMap<>();
		}
		if (value == null) {
			styles.remove(name);
		} else {
			styles.put(name, value);
		}
		if (element != null) {
			if (value == null) {
				((Element) element).getStyle().removeProperty(name.nameValid());
			} else {
				((Element) element).getStyle().setProperty(name.nameValid(), value);
			}
		}
		return (Fluent) this;
	}

	public String css(Css name) {
		if (styles == null) {
			return null;
		}
		return styles.get(name);
	}

	public Fluent att(Att name, String value, Att name2, String value2) {
		return att(name, value).att(name2, value2);
	}

	public Fluent att(Att name, String value, Att name2, String value2, Att name3, String value3) {
		return att(name, value).att(name2, value2).att(name3, value3);
	}

	/**
	 * Set or remove (by value null) an attribute.
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public Fluent att(Att name, String value) {
		if (name == null) { // ignoring the call
			return (Fluent) this;
		}
		if (attrs == null) {
			attrs = new TreeMap<>();
		}
		if (value == null) {
			if (attrs.containsKey(name)) {
				attrs.remove(name);
				if (element != null) {

					switch (name) {
					case checked:
						((InputElement) element).setChecked(false);
						break;
					case value:
						((InputElement) element).setValue(null);
						break;
					default:
						((Element) element).removeAttribute(name.nameValid());
						break;
					}

				}
			}
		} else {
			// TODO do not set when value is the same, also elsewhere
			attrs.put(name, value);
			if (element != null) {

				switch (name) {
				case checked:
					((InputElement) element).setChecked(true);
					break;
				case value:
					((InputElement) element).setValue(value);
					break;
				default:
					((Element) element).setAttribute(name.nameValid(), value);
					break;
				}

			}
		}
		return (Fluent) this;
	}

	/**
	 * Get the value of an input field; use att() to set the value.
	 */
	public String domValue() {
		return ((InputElement) element).getValue();
	}

	/**
	 * Get whether an input with type checkbox is checked; use att() to set the
	 * value.
	 */
	public boolean domChecked() {
		return ((InputElement) element).isChecked();
	}

	/**
	 * Get the selected index of a select node.
	 */
	public int domSelectedIndex() {
		return ((SelectElement) element).getSelectedIndex();
	}

	public String att(Att name) {
		if (attrs == null) {
			return null;
		}
		return attrs.get(name);
	}

	public String id() {
		return att(Att.id);
	}

	public Fluent id(String string) {
		return att(Att.id, string);
	}

	public String classs() {
		return att(Att.class_);
	}

	public Fluent classs(String string) {
		return att(Att.class_, string);
	}

	public String tag() {
		return tag;
	}

	private void addNew(Viewable item) {
		if (childs == null) {
			childs = new ArrayList<>();
		}
		if (item instanceof ViewOnBase) {
			((ViewOnBase) item).setParent((Fluent) this);
			((ViewOnBase) item).sync(); // needs to render!
		} else {
			item = getRootOf((Fluent) item);

			// This line connects staticly created Fluents to the DOM.
			// see for comments for the if-statement below in ViewOnBase::sync()
			if (!GWT.isClient() || element != null) {
				Renderer.syncChild((Fluent) this, item, null);
			}
		}
		childs.add(item);
	}

	protected static Fluent getRootOf(Fluent item) {
		// When a Fluent created by a static function is given, we should
		// get the most upper parent, not the last item of the fluent
		// notated item.
		if (item == null) {
			return null;
		}
		while (item.parent != null) {
			if (item.element != null) {
				throw new IllegalArgumentException("Can not reconnect connected DOM elements");
			}
			item = item.parent;
		}
		return item;
	}

	/**
	 * Try not to use this method, use the fluent methods or the cosntructors
	 * instead.
	 */
	public Fluent add(Viewable... items) {
		for (Viewable item : items) {
			addNew(item);
		}
		return (Fluent) this;
	}

	/**
	 * Try not to use this method, use the fluent methods or the cosntructors
	 * instead.
	 */
	public Fluent add(Stream<Fluent> stream) {
		stream.forEach(item -> addNew(item));
		return (Fluent) this;
	}

	public List<Viewable> getChildren() {
		return childs;
	}

	public <T> ViewOn<T> add(T initialState, Function<T, Fluent> method) {
		ViewOn<T> result = new ViewOn<T>(initialState, method);
		addNew(result);
		return result;
	}

	public <A, B> ViewOnBoth<A, B> add(A initialState1, B initialState2, BiFunction<A, B, Fluent> method) {
		ViewOnBoth<A, B> result = new ViewOnBoth<A, B>(initialState1, initialState2, method);
		addNew(result);
		return result;
	}

	@Override
	public String toString() {
		String result = "Fluent{<";
		if (tag == null) {
			result += "null";
		} else {
			result += tag;
		}
		if (attrs != null) {
			for (Att attr : attrs.keySet()) {
				result += " " + attr.nameValid() + "=" + attrs.get(attr);
			}
		}
		result += " /> el=";
		if (element == null) {
			result += "null";
		} else {
			result += element.getNodeName();
		}
		result += ", parent.tag=";
		if (parent != null) {
			result += parent.tag;
		}
		result += "}";
		return result;
	}

	public void disabled(boolean disabled) {
		if (disabled) {
			att(Att.disable, "");
		} else {
			att(Att.disable, null);
		}
	}

	// UNIT TESTING
	// UNIT TESTING
	// UNIT TESTING

	/**
	 * Clean the DOM manually before the next junit test.
	 */
	public static void clearVirtualDOM() {
		if (!GWT.isClient()) {
			body = new Fluent(null);
		} else {
			throw new IllegalArgumentException(
					"Calling this method has zero meaning inside your browser, reload the page in your browser for a clean start.");
		}
	}

	public Fluent clone() {
		if (parent != null || element != null) {
			throw new IllegalArgumentException(
					"You can only clone objects created with a static method (which start with a capital letter like Div or Span) and which are not DOM-connected yet.");
		}
		Fluent result = new Fluent(tag, null);
		if (text != null) {
			result.txt(text);
		}
		if (attrs != null) {
			for (Att att : attrs.keySet()) {
				result.att(att, attrs.get(att));
			}
		}
		if (styles != null) {
			for (Css name : styles.keySet()) {
				result.css(name, styles.get(name));
			}
		}
		if (listeners != null) {
			for (String name : listeners.keySet()) {
				result.listen(name, listeners.get(name));
			}
		}
		if (childs != null) {
			for (Viewable child : childs) {
				if (child instanceof Fluent) {
					result.add(((Fluent) child).clone());
				} else if (child instanceof ViewOn<?>) {
					result.add(((ViewOn<?>) child).clone());
				} else {
					result.add(((ViewOnBoth<?, ?>) child).clone());
				}
			}
		}
		return result;
	}

	/*
	 * This does not cover listeners (which is not possible in GWT production
	 * mode), but does cover the rest, which should be enough to identify which
	 * child is which for most render optimalisation issues. For example, when
	 * one child between other childs in a list is deleted.
	 */
	@Override
	public String getCrcString() {
		StringBuilder result = new StringBuilder();
		if (tag != null) {
			result.append(tag);
		}
		if (text != null) {
			result.append(text);
		}
		if (attrs != null) {
			for (Att att : attrs.keySet()) {
				result.append(att.name());
			}
		}
		if (childs != null) {
			// only three deep
			// byte[] cache = new byte[8];
			for (int x = 0; x < 3 && x < childs.size(); x++) {
				result.append(childs.get(x).getCrcString());
				// for (int i = 7; i >= 0; i--) {
				// cache[i] = (byte) (child & 0xFF);
				// child >>= 8;
				// }
				// result.append(cache);
			}
		}
		return result.toString();
	}

	@Override
	public int getCrc() {
		Fluent.console.log(getCrcString());
		return getCrcString().hashCode();
	}

}