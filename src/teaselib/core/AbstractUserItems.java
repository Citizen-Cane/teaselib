package teaselib.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import teaselib.Body;
import teaselib.Clothes;
import teaselib.Gadgets;
import teaselib.Household;
import teaselib.Posture;
import teaselib.Toys;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.ReflectionUtils;
import teaselib.util.Item;
import teaselib.util.ItemImpl;

public abstract class AbstractUserItems implements UserItems {
    private static final String ITEMS_DTD = "items.dtd";

    protected final TeaseLib teaseLib;
    final Map<String, ItemMap> domainMap = new HashMap<>();

    class ItemMap extends HashMap<Object, List<Item>> {
        private static final long serialVersionUID = 1L;
    }

    public enum Settings {
        ITEM_STORE;
    }

    public AbstractUserItems(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    List<Item> userItems;

    private List<Item> loadItems(File file) throws IOException {
        List<Item> items = new ArrayList<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver((publicId, systemId) -> {
                if (systemId.contains(ITEMS_DTD)) {
                    return new InputSource(AbstractUserItems.class.getResourceAsStream(ITEMS_DTD));
                } else {
                    return null;
                }
            });

            Document dom = db.parse(file);
            Element doc = dom.getDocumentElement();

            Node itemClass = doc.getFirstChild();
            for (; itemClass != null; itemClass = itemClass.getNextSibling()) {
                if (itemClass.getNodeType() == Node.ELEMENT_NODE) {
                    Node itemNode = itemClass.getFirstChild();
                    for (; itemNode != null; itemNode = itemNode.getNextSibling()) {
                        if ("Item".equalsIgnoreCase(itemNode.getNodeName())
                                && itemNode.getNodeType() == Node.ELEMENT_NODE) {
                            items.add(readItem(itemClass, itemNode));
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | ClassNotFoundException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }

        return items;
    }

    private ItemImpl readItem(Node itemClass, Node itemNode) throws ClassNotFoundException {
        NamedNodeMap attributes = itemNode.getAttributes();
        String itemName = attributes.getNamedItem("item").getNodeValue();
        String guid = attributes.getNamedItem("guid").getNodeValue();
        String displayName = attributes.getNamedItem("displayName").getNodeValue();

        List<Enum<?>> itemAttributes = new ArrayList<>();
        NodeList childNodes = itemNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node attributeNode = childNodes.item(i);
            if (attributeNode.getNodeType() == Node.ELEMENT_NODE) {
                if ("Attribute".equalsIgnoreCase(attributeNode.getNodeName())) {
                    String enumClassName = "teaselib." + attributeNode.getTextContent().trim();
                    itemAttributes.add(ReflectionUtils.getEnum(QualifiedItem.of(enumClassName)));
                }
            }
        }

        String enumName = "teaselib." + itemClass.getNodeName() + "." + itemName;
        Enum<?> enumValue = ReflectionUtils.getEnum(QualifiedItem.of(enumName));
        return new ItemImpl(teaseLib, enumValue, TeaseLib.DefaultDomain, //
                guid, //
                displayName, defaults(new QualifiedEnum(enumValue)), //
                itemAttributes.toArray());
    }

    protected Item[] getDefaultItem(String domain, QualifiedItem item) {
        return new Item[] {
                new ItemImpl(teaseLib, item.value(), domain, item.name(), ItemImpl.createDisplayName(item)) };
    }

    @Override
    public List<Item> get(String domain, QualifiedItem item) {
        ItemMap itemMap = domainMap.get(domain);
        if (itemMap == null) {
            itemMap = new ItemMap();
            domainMap.put(domain, itemMap);
        }

        if (userItems == null) {
            if (teaseLib.config.has(Settings.ITEM_STORE)) {
                try {
                    // TODO domain parameter
                    userItems = loadItems(new File(teaseLib.config.get(Settings.ITEM_STORE)));
                } catch (IOException e) {
                    throw ExceptionUtil.asRuntimeException(e);
                }
            } else {
                userItems = Collections.emptyList();
            }
        }

        if (!itemMap.containsKey(item)) {
            List<Item> allItems = getUserItems();
            List<Item> items = allItems.stream().filter(itemImpl -> item.equals(((ItemImpl) itemImpl).item))
                    .collect(Collectors.toList());
            if (!items.isEmpty()) {
                itemMap.put(item, items);
                return items;
            } else {
                List<Item> defaults = Arrays.asList(createDefaultItems(domain, item));
                itemMap.put(item, defaults);
                return defaults;
            }
        } else {
            return itemMap.get(item);
        }
    }

    private List<Item> getUserItems() {
        return userItems;
    }

    protected abstract Item[] createDefaultItems(String domain, QualifiedItem item);

    protected Item item(QualifiedItem item, String name, String displayName, Enum<?>... attributes) {
        return item(TeaseLib.DefaultDomain, name, displayName, item, defaults(item), attributes);
    }

    protected Item item(QualifiedItem item, String name, String displayName, Enum<?>[] defaultPeers,
            Enum<?>... attributes) {
        return item(TeaseLib.DefaultDomain, name, displayName, item, defaultPeers, attributes);
    }

    protected Item item(String domain, String name, String displayName, QualifiedItem item, Enum<?>[] defaultPeers,
            Enum<?>... attributes) {
        return new ItemImpl(teaseLib, item.value(), domain, name, displayName, defaultPeers, attributes);
    }

    public Enum<?>[] array(Enum<?>[] defaults, Enum<?>... additional) {
        Enum<?>[] extended = new Enum<?>[defaults.length + additional.length];
        System.arraycopy(defaults, 0, extended, 0, defaults.length);
        System.arraycopy(additional, 0, extended, defaults.length, additional.length);
        return extended;
    }

    @Override
    public Enum<?>[] defaults(QualifiedItem item) {
        if (item.namespace().equalsIgnoreCase(Toys.class.getName())) {
            return getToyDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Clothes.class.getName())) {
            return getClothesDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Household.class.getName())) {
            return getHouseholdDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Gadgets.class.getName())) {
            return getGadgetsDefaults(item);
        } else {
            throw new IllegalArgumentException("Defaults not defined for " + item);
        }
    }

    private static Enum<?>[] getClothesDefaults(QualifiedItem item) {
        return new Body[] {};
    }

    private static Enum<?>[] getHouseholdDefaults(QualifiedItem item) {
        return new Body[] {};
    }

    private static Enum<?>[] getGadgetsDefaults(QualifiedItem item) {
        return new Body[] {};
    }

    /**
     * Get system default peers for items. The defaults depend on the meaning of the item. Item have defaults if they
     * have a clear application or are applied to a distinct spot, like putting a collar around one's neck. Same for
     * ankle restraints.
     * <p>
     * On the other hand wrist restraints don't have a default, since wrists can be tied before and behind the body.
     * 
     * @param item
     *            The item to get defaults for.
     * @return The defaults for the item. An item may not have defaults, in this case the returned array is empty.
     */
    private static Enum<?>[] getToyDefaults(QualifiedItem item) {
        if (item.equals(Toys.Buttplug)) {
            return new Body[] { Body.InButt };
        } else if (item.equals(Toys.Ankle_Restraints)) {
            return new Body[] { Body.AnklesTied };
        } else if (item.equals(Toys.Wrist_Restraints)) {
            return new Body[] { Body.WristsTied };
        } else if (item.equals(Toys.Gag)) {
            return new Body[] { Body.InMouth };
        } else if (item.equals(Toys.Spanking_Implement)) {
            return new Body[] {};
        } else if (item.equals(Toys.Collar)) {
            return new Body[] { Body.AroundNeck };
        } else if (item.equals(Toys.Nipple_Clamps)) {
            return new Body[] { Body.OnNipples };
        } else if (item.equals(Toys.Chastity_Device)) {
            return new Body[] { Body.OnPenis, Body.CantJerkOff };
        } else if (item.equals(Toys.Dildo)) {
            return new Body[] {};
        } else if (item.equals(Toys.VaginalInsert)) {
            return new Body[] { Body.InVagina };
        } else if (item.equals(Toys.Vibrator)) {
            return new Body[] {};
        } else if (item.equals(Toys.Ball_Stretcher)) {
            return new Body[] { Body.OnBalls };
        } else if (item.equals(Toys.Blindfold)) {
            return new Body[] { Body.Blindfolded };
        } else if (item.equals(Toys.Cockring)) {
            return new Body[] { Body.AroundCockBase };
        } else if (item.equals(Toys.Anal_Douche)) {
            return new Body[] { Body.InButt };
        } else if (item.equals(Toys.Enema_Bulb)) {
            return new Body[] { Body.InButt };
        } else if (item.equals(Toys.Enema_Kit)) {
            return new Body[] { Body.InButt };
        } else if (item.equals(Toys.GlansRing)) {
            return new Enum<?>[] { Body.OnPenis };
        } else if (item.equals(Toys.Humbler)) {
            return new Enum<?>[] { Body.OnBalls, Posture.CantStand, Posture.CantSitOnChair };
        } else if (item.equals(Toys.Masturbator)) {
            return new Body[] {};
        } else if (item.equals(Toys.Pussy_Clamps)) {
            return new Body[] { Body.OnLabia, Body.OnBalls };
        } else if (item.equals(Toys.Clit_Clamp)) {
            return new Body[] { Body.OnClit, Body.OnPenis };
        } else if (item.equals(Toys.Spreader_Bar)) {
            return new Body[] {};
        } else if (item.equals(Toys.EStim_Device)) {
            return new Body[] {};
        } else if (item.equals(Toys.Chains)) {
            return new Body[] {};
        } else if (item.equals(Toys.Rope)) {
            return new Body[] {};
        } else if (item.equals(Toys.Doll)) {
            return new Body[] {};
        } else if (item.equals(Toys.Husband)) {
            return new Body[] {};
        } else if (item.equals(Toys.Wife)) {
            return new Body[] {};
        } else if (item.equals(Toys.Strap_On)) {
            return new Body[] {};
        } else {
            throw new IllegalArgumentException("Defaults not defined for " + item);
        }
    }
}
