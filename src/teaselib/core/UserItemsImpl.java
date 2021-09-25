package teaselib.core;

import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import teaselib.Accessoires;
import teaselib.Body;
import teaselib.Bondage;
import teaselib.Clothes;
import teaselib.Gadgets;
import teaselib.Household;
import teaselib.Posture;
import teaselib.Shoes;
import teaselib.Toys;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.ReflectionUtils;
import teaselib.util.Item;
import teaselib.util.ItemGuid;
import teaselib.util.ItemImpl;

public class UserItemsImpl implements UserItems {
    private static final String ITEMS_DTD = "items.dtd";

    protected final TeaseLib teaseLib;
    private final Map<String, ItemMap> domainMap = new HashMap<>();
    List<URL> loadOrder = new ArrayList<>();

    class ItemMap extends LinkedHashMap<Object, Map<String, Item>> {
        private static final long serialVersionUID = 1L;

        public Map<String, Item> getOrDefault(Object key, Supplier<Map<String, Item>> defaultSupplier) {
            Map<String, Item> value = super.get(key);
            if (value == null) {
                value = defaultSupplier.get();
                put(key, value);
            }
            return value;
        }
    }

    public enum Settings {
        ITEM_DEFAULT_STORE,
        ITEM_USER_STORE
    }

    public UserItemsImpl(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;

        if (loadOrder.isEmpty()) {
            addItems(getClass().getResource(teaseLib.config.get(Settings.ITEM_DEFAULT_STORE)));
            if (teaseLib.config.has(Settings.ITEM_USER_STORE)) {
                try {
                    addItems(Paths.get(teaseLib.config.get(Settings.ITEM_USER_STORE)).toUri().toURL());
                } catch (MalformedURLException e) {
                    throw ExceptionUtil.asRuntimeException(e);
                }
            }
        }
    }

    @Override
    public void addItems(URL url) {
        Objects.requireNonNull(url);

        loadOrder.add(url);
        clearCachedItems();
    }

    public void clearCachedItems() {
        domainMap.clear();
    }

    public void clearLoadOrder() {
        loadOrder.clear();
    }

    private List<ItemImpl> readItems(String domain, URL url) throws IOException {
        List<ItemImpl> items = new ArrayList<>();

        var dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver((publicId, systemId) -> {
                if (systemId.contains(ITEMS_DTD)) {
                    return new InputSource(UserItemsImpl.class.getResourceAsStream(ITEMS_DTD));
                } else {
                    return null;
                }
            });

            Document dom;
            try (InputStream data = url.openStream()) {
                dom = db.parse(data);
            }

            var doc = dom.getDocumentElement();
            Node itemClass = doc.getFirstChild();
            for (; itemClass != null; itemClass = itemClass.getNextSibling()) {
                if (itemClass.getNodeType() == Node.ELEMENT_NODE) {
                    var itemNode = itemClass.getFirstChild();
                    for (; itemNode != null; itemNode = itemNode.getNextSibling()) {
                        if ("Item".equalsIgnoreCase(itemNode.getNodeName())
                                && itemNode.getNodeType() == Node.ELEMENT_NODE) {
                            items.add(readItem(domain, itemClass, itemNode));
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }

        return items;
    }

    private ItemImpl readItem(String domain, Node itemClass, Node itemNode) {
        NamedNodeMap attributes = itemNode.getAttributes();
        String itemName = attributes.getNamedItem("item").getNodeValue();
        String guid = attributes.getNamedItem("guid").getNodeValue();
        String displayName = attributes.getNamedItem("displayName").getNodeValue();

        String namespace = "teaselib." + itemClass.getNodeName();
        String enumName = namespace + "." + itemName;
        Enum<?> enumValue = ReflectionUtils.getEnum(QualifiedItem.of(enumName));
        List<Enum<?>> defaultPeers = new ArrayList<>(Arrays.asList(defaults(new QualifiedEnum(enumValue))));
        List<Enum<?>> itemAttributes = new ArrayList<>();

        NodeList childNodes = itemNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            var node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if ("Attribute".equalsIgnoreCase(node.getNodeName())) {
                    String enumClassName = "teaselib." + node.getTextContent().trim();
                    itemAttributes.add(ReflectionUtils.getEnum(QualifiedItem.of(enumClassName)));
                }
                if ("DefaultPeer".equalsIgnoreCase(node.getNodeName())) {
                    String enumClassName = "teaselib." + node.getTextContent().trim();
                    defaultPeers.add(ReflectionUtils.getEnum(QualifiedItem.of(enumClassName)));
                }
            }
        }

        QualifiedItem kind = new QualifiedEnum(enumValue);
        return new ItemImpl(teaseLib, domain, ItemGuid.from(kind, guid), //
                displayName, defaultPeers.toArray(new Enum<?>[defaultPeers.size()]), //
                itemAttributes.toArray(new Enum<?>[itemAttributes.size()]));
    }

    private ItemImpl getDefaultItem(String domain, QualifiedItem kind) {
        return new ItemImpl(teaseLib, domain, ItemGuid.from(kind, kind.name()), ItemImpl.createDisplayName(kind));
    }

    @Override
    public List<Item> get(String domain, QualifiedItem item) {
        var itemMap = getItemMap(domain);
        List<Item> all = collectItems(item, itemMap);

        if (all.isEmpty()) {
            if (item.guid().isEmpty()) {
                addDefaultItem(domain, item, itemMap, all);
            } else {
                all.add(Item.NotFound);
            }
        }

        return Collections.unmodifiableList(all);

    }

    private ItemMap getItemMap(String domain) {
        var itemMap = domainMap.get(domain);

        if (itemMap == null) {
            itemMap = new ItemMap();
            domainMap.put(domain, itemMap);
            try {
                loadItems(domain, itemMap);
            } catch (IOException e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
        }

        return itemMap;
    }

    private void loadItems(String domain, ItemMap itemMap) throws IOException {
        for (URL url : loadOrder) {
            List<ItemImpl> items = readItems(domain, url);
            addItems(itemMap, items);
        }
    }

    private static void addItems(ItemMap itemMap, List<ItemImpl> items) {
        for (ItemImpl item : items) {
            Map<String, Item> allItemsOfThisType = itemMap.getOrDefault(item.value(), LinkedHashMap<String, Item>::new);
            allItemsOfThisType.put(item.guid.name(), item);
        }
    }

    private static List<Item> collectItems(QualifiedItem qualifiedItem, ItemMap itemMap) {
        List<Item> all = new ArrayList<>();

        // // TODO use QualifiedItem as key for item map - requires ItemMap to use QualifiedItem
        // Map<String, Item> items = itemMap.get(new QualifiedString(qualifiedItem.namespace(), qualifiedItem.name()));
        // if (items != null) {
        // Optional<String> guid = qualifiedItem.guid();
        // if (guid.isPresent()) {
        // all.addAll(itemsMatchingGuid(items, guid.get()));
        // } else {
        // all.addAll(items.values());
        // }
        // }

        for (Entry<Object, Map<String, Item>> entry : itemMap.entrySet()) {
            var key = new QualifiedString(qualifiedItem.namespace(), qualifiedItem.name());
            if (key.equals(entry.getKey())) {
                Optional<String> guid = qualifiedItem.guid();
                if (guid.isPresent()) {
                    all.addAll(itemsMatchingGuid(entry.getValue(), guid.get()));
                } else {
                    all.addAll(entry.getValue().values());
                }
            }
        }

        return all;
    }

    private static List<Item> itemsMatchingGuid(Map<String, Item> items, String guid) {
        return items.values().stream().map(ItemImpl.class::cast).filter(item -> item.guid.name().equals(guid))
                .collect(toList());
    }

    private void addDefaultItem(String domain, QualifiedItem kind, ItemMap itemMap, List<Item> all) {
        List<ItemImpl> defaultItems = Collections.singletonList(getDefaultItem(domain, kind));
        addItems(itemMap, defaultItems);
        all.addAll(defaultItems);
    }

    @Override
    public Enum<?>[] defaults(QualifiedItem item) {
        if (item.namespace().equalsIgnoreCase(Accessoires.class.getName())) {
            return getAccessoiresDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Bondage.class.getName())) {
            return getBondageDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Clothes.class.getName())) {
            return getClothesDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Gadgets.class.getName())) {
            return getGadgetsDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Household.class.getName())) {
            return getHouseholdDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Shoes.class.getName())) {
            return getShoesDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Toys.class.getName())) {
            return getToyDefaults(item);
        } else {
            throw new IllegalArgumentException("Defaults not defined for " + item);
        }
    }

    private static Enum<?>[] getAccessoiresDefaults(QualifiedItem item) {
        if (item.is(Accessoires.Breast_Forms)) {
            return new Body[] { Body.OnNipples };
        } else {
            return new Body[] {};
        }
    }

    private static Enum<?>[] getBondageDefaults(QualifiedItem item) {
        if (item.is(Bondage.Chains)) {
            return new Body[] {};
        } else if (item.is(Bondage.Rope)) {
            return new Body[] {};
        } else if (item.is(Bondage.Spreader_Bar)) {
            return new Body[] {};
        } else if (item.is(Bondage.Anklets)) {
            return new Body[] { Body.AnklesCuffed };
        } else if (item.is(Bondage.Wristlets)) {
            return new Body[] { Body.WristsCuffed };
        } else {
            return new Body[] {};
        }
    }

    private static Enum<?>[] getClothesDefaults(@SuppressWarnings("unused") QualifiedItem item) {
        return new Body[] {};
    }

    private static Enum<?>[] getGadgetsDefaults(@SuppressWarnings("unused") QualifiedItem item) {
        return new Body[] {};
    }

    private static Enum<?>[] getHouseholdDefaults(@SuppressWarnings("unused") QualifiedItem item) {
        return new Body[] {};
    }

    private static Enum<?>[] getShoesDefaults(@SuppressWarnings("unused") QualifiedItem item) {
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
        if (item.is(Toys.Buttplug)) {
            return new Body[] { Body.InButt };
        } else if (item.is(Toys.Ankle_Restraints)) {
            return new Body[] { Body.AnklesCuffed, Body.AnklesTied };
        } else if (item.is(Toys.Wrist_Restraints)) {
            return new Body[] { Body.WristsCuffed, Body.WristsTied };
        } else if (item.is(Toys.Gag)) {
            return new Body[] { Body.InMouth };
        } else if (item.is(Toys.Spanking_Implement)) {
            return new Body[] {};
        } else if (item.is(Toys.Collar)) {
            return new Body[] { Body.AroundNeck };
        } else if (item.is(Toys.Nipple_Clamps)) {
            return new Body[] { Body.OnNipples };
        } else if (item.is(Toys.Chastity_Device)) {
            return new Body[] { Body.OnPenis, Body.CantJerkOff };
        } else if (item.is(Toys.Dildo)) {
            return new Body[] {};
        } else if (item.is(Toys.VaginalInsert)) {
            return new Body[] { Body.InVagina };
        } else if (item.is(Toys.Vibrator)) {
            return new Body[] {};
        } else if (item.is(Toys.Ball_Stretcher)) {
            return new Body[] { Body.OnBalls };
        } else if (item.is(Toys.Blindfold)) {
            return new Body[] { Body.Blindfolded };
        } else if (item.is(Toys.Cock_Ring)) {
            return new Body[] { Body.AroundCockBase };
        } else if (item.is(Toys.Anal_Douche)) {
            return new Body[] { Body.InButt };
        } else if (item.is(Toys.Enema_Bulb)) {
            return new Body[] { Body.InButt };
        } else if (item.is(Toys.Enema_Kit)) {
            return new Body[] { Body.InButt };
        } else if (item.is(Toys.Glans_Ring)) {
            return new Enum<?>[] { Body.OnPenis };
        } else if (item.is(Toys.Humbler)) {
            return new Enum<?>[] { Body.OnBalls, Posture.CantStand, Posture.CantSitOnChair };
        } else if (item.is(Toys.Masturbator)) {
            return new Body[] {};
        } else if (item.is(Toys.Pussy_Clamps)) {
            return new Body[] { Body.OnLabia, Body.OnBalls };
        } else if (item.is(Toys.Clit_Clamp)) {
            return new Body[] { Body.OnClit, Body.OnPenis };
        } else if (item.is(Toys.EStim_Device)) {
            return new Body[] {};
        } else if (item.is(Toys.Doll)) {
            return new Body[] {};
        } else if (item.is(Toys.Spouse)) {
            return new Body[] {};
        } else {
            throw new IllegalArgumentException("Defaults not defined for " + item);
        }
    }
}
