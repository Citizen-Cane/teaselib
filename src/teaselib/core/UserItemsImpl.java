package teaselib.core;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import teaselib.Features;
import teaselib.Gadgets;
import teaselib.Household;
import teaselib.Posture;
import teaselib.Shoes;
import teaselib.Toys;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.FileUtilities;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.QualifiedStringMapping;
import teaselib.util.Item;

public class UserItemsImpl implements UserItems {

    public enum Settings {
        ITEM_DEFAULT_STORE,
        ITEM_USER_STORE,
        USER_ITEMS_PATH,
        SCRIPT_ITEMS_PATH,
    }

    private static final String ITEM_EXTENSION = ".xml";

    private static boolean isUserItemsFile(File file) {
        return file.getName().endsWith(ITEM_EXTENSION);
    }

    private static final String ITEMS_DTD = "items.dtd";

    protected final TeaseLib teaseLib;
    private final Map<String, ItemMap> domainMap = new HashMap<>();
    List<URL> defaults = new ArrayList<>();

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

    public UserItemsImpl(TeaseLib teaseLib) throws IOException {
        this.teaseLib = teaseLib;
        addDefaultUserItems();
    }

    private void addDefaultUserItems() throws IOException {
        addUserItems(getClass().getResource(teaseLib.config.get(Settings.ITEM_DEFAULT_STORE)));
        if (teaseLib.config.has(Settings.ITEM_USER_STORE)) {
            addUserItems(Paths.get(teaseLib.config.get(Settings.ITEM_USER_STORE)).toUri().toURL());
        }
    }

    public void addUserItems(URL url) {
        Objects.requireNonNull(url);
        defaults.add(url);
        clearCachedItems();
    }

    @Override
    public void addItems(URL url) {
        Objects.requireNonNull(url);
        if (teaseLib.config.has(Settings.SCRIPT_ITEMS_PATH)) {
            try {
                File inventoryPath = new File(teaseLib.config.get(Settings.SCRIPT_ITEMS_PATH));
                inventoryPath.mkdirs();
                String name = new File(url.getPath()).getName();
                addUserFileOnce(url, new File(inventoryPath, name));
            } catch (IOException e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
        } else {
            defaults.add(url);
        }
        clearCachedItems();
    }

    public void addUserFileOnce(URL url, File userFile) throws IOException {
        if (!userFile.exists()) {
            FileUtilities.copy(url, userFile);
        }
    }

    @Override
    public void addItems(Collection<Item> items) {
        ItemMap itemMap = domainMap.get(TeaseLib.DefaultDomain);
        List<ItemImpl> instances = items.stream().map(ItemImpl.class::cast).toList();
        addItems(itemMap, instances);
    }

    public void clearCachedItems() {
        domainMap.clear();
    }

    public void clearLoadOrder() {
        defaults.clear();
        try {
            addDefaultUserItems();
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    private List<ItemImpl> loadItems(String domain, URL url) throws IOException {
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

        String namespace = "teaselib." + itemClass.getNodeName();
        String itemName = attributes.getNamedItem("item").getNodeValue();
        var kind = QualifiedString.of(namespace + "." + itemName);
        Set<QualifiedString> defaultPeers = QualifiedStringMapping.of(defaults(kind));
        Set<QualifiedString> itemAttributes = new HashSet<>();
        Set<QualifiedString> itemBlockers = QualifiedStringMapping.of(itemBlockers(kind));

        NodeList childNodes = itemNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            var node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if ("Attribute".equalsIgnoreCase(node.getNodeName())) {
                    String name = "teaselib." + node.getTextContent().trim();
                    itemAttributes.add(QualifiedString.of(name));
                } else //
                if ("DefaultPeer".equalsIgnoreCase(node.getNodeName())) {
                    String name = "teaselib." + node.getTextContent().trim();
                    defaultPeers.add(QualifiedString.of(name));
                } else //
                if ("BlockedBy".equalsIgnoreCase(node.getNodeName())) {
                    String name = "teaselib." + node.getTextContent().trim();
                    defaultPeers.add(QualifiedString.of(name));
                }
            }
        }
        applyPeerRules(kind, defaultPeers, itemAttributes, itemBlockers);
        relaxBlockingRules(kind, defaultPeers, itemAttributes, itemBlockers);

        String guid = attributes.getNamedItem("guid").getNodeValue();
        QualifiedString item = QualifiedString.from(kind, guid);
        String displayName = attributes.getNamedItem("displayName").getNodeValue();
        return new ItemImpl(teaseLib, domain, item, displayName, defaultPeers, itemAttributes, itemBlockers);
    }

    private ItemImpl getDefaultItem(String domain, QualifiedString kind) {
        QualifiedString item = QualifiedString.from(kind, kind.name());
        return new ItemImpl(teaseLib, domain, item, ItemImpl.createDisplayName(item));
    }

    @Override
    public List<Item> get(String domain, QualifiedString item) {
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
        List<List<ItemImpl>> all = new ArrayList<>();
        all.addAll(defaultItems(domain));
        all.addAll(userItems(domain, Settings.USER_ITEMS_PATH));
        all.addAll(userItems(domain, Settings.SCRIPT_ITEMS_PATH));
        for (var items : all) {
            addItems(itemMap, items);
        }
    }

    private List<List<ItemImpl>> defaultItems(String domain) throws IOException {
        List<List<ItemImpl>> e = new ArrayList<>();
        for (var def : defaults) {
            e.add(loadItems(domain, def));
        }
        return e;
    }

    private List<List<ItemImpl>> userItems(String domain, Settings path) throws MalformedURLException, IOException {
        if (teaseLib.config.has(path)) {
            File inventoryPath = new File(teaseLib.config.get(path));
            File[] files = inventoryPath.listFiles(UserItemsImpl::isUserItemsFile);
            if (files == null) {
                return Collections.emptyList();
            } else {
                List<List<ItemImpl>> all = new ArrayList<>();
                for (var file : files) {
                    URL url = file.toURI().toURL();
                    List<ItemImpl> items = loadItems(domain, url);
                    all.add(items);
                }
                return all;
            }
        } else {
            return Collections.emptyList();
        }
    }

    private static void addItems(ItemMap itemMap, List<ItemImpl> items) {
        for (ItemImpl item : items) {
            Map<String, Item> allItemsOfThisType = itemMap.getOrDefault(item.kind(), LinkedHashMap<String, Item>::new);
            allItemsOfThisType.put(item.name.guid().orElseThrow(), item);
        }
    }

    private static List<Item> collectItems(QualifiedString qualifiedItem, ItemMap itemMap) {
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

        var key = qualifiedItem.kind();
        for (Entry<Object, Map<String, Item>> entry : itemMap.entrySet()) {
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
        return items.values().stream().map(ItemImpl.class::cast)
                .filter(item -> item.name.guid().orElseThrow().equals(guid)).collect(toList());
    }

    private void addDefaultItem(String domain, QualifiedString kind, ItemMap itemMap, List<Item> all) {
        List<ItemImpl> defaultItems = Collections.singletonList(getDefaultItem(domain, kind));
        addItems(itemMap, defaultItems);
        all.addAll(defaultItems);
    }

    @Override
    public Enum<?>[] defaults(QualifiedString item) {
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

    private static Enum<?>[] getAccessoiresDefaults(QualifiedString item) {
        if (item.is(Accessoires.Breast_Forms)) {
            return peers(Body.OnNipples);
        } else {
            return None;
        }
    }

    private static Enum<?>[] getBondageDefaults(QualifiedString item) {
        if (item.is(Bondage.Chains)) {
            return None;
        } else if (item.is(Bondage.Rope)) {
            return None;
        } else if (item.is(Bondage.Spreader_Bar)) {
            return None;
        } else if (item.is(Bondage.Anklets)) {
            return peers(Body.AnklesCuffed);
        } else if (item.is(Bondage.Wristlets)) {
            return peers(Body.WristsCuffed);
        } else {
            return None;
        }
    }

    private static Enum<?>[] getClothesDefaults(@SuppressWarnings("unused") QualifiedString item) {
        return None;
    }

    private static Enum<?>[] getGadgetsDefaults(@SuppressWarnings("unused") QualifiedString item) {
        return None;
    }

    private static Enum<?>[] getHouseholdDefaults(@SuppressWarnings("unused") QualifiedString item) {
        return None;
    }

    private static Enum<?>[] getShoesDefaults(@SuppressWarnings("unused") QualifiedString item) {
        return None;
    }

    /**
     * Get system default peers for items. The defaults depend on the meaning of the item. Item have defaults if they
     * have a clear application or are applied to a distinct spot, like putting a collar around one's neck. Same for
     * ankle restraints.
     * <p>
     * On the other hand wrist restraints don't have a Posture default, since wrists can be tied before and behind the
     * body.
     * 
     * @param item
     *            The item to get defaults for.
     * @return The defaults for the item. An item may not have defaults, in this case the returned array is empty.
     */
    private static Enum<?>[] getToyDefaults(QualifiedString item) {
        if (item.is(Toys.Buttplug)) {
            return peers(Body.InButt);
        } else if (item.is(Toys.Ankle_Restraints)) {
            return peers(Body.AnklesCuffed, Body.AnklesTied, Posture.AnklesTiedTogether);
        } else if (item.is(Toys.Wrist_Restraints)) {
            return peers(Body.WristsCuffed, Body.WristsTied);
        } else if (item.is(Toys.Gag)) {
            return peers(Body.InMouth);
        } else if (item.is(Toys.Spanking_Implement)) {
            return None;
        } else if (item.is(Toys.Collar)) {
            return peers(Body.AroundNeck);
        } else if (item.is(Toys.Nipple_Clamps)) {
            return peers(Body.OnNipples);
        } else if (item.is(Toys.Chastity_Device)) {
            return peers( //
                    Body.AroundCockBase, // Not for chastity belts with crotch band
                    Body.OnPenis, //
                    Body.CantJerkOff);
        } else if (item.is(Toys.Dildo)) {
            return None;
        } else if (item.is(Toys.VaginalInsert)) {
            return peers(Body.InVagina);
        } else if (item.is(Toys.Vibrator)) {
            return None;
        } else if (item.is(Toys.Ball_Stretcher)) {
            return peers(Body.OnBalls);
        } else if (item.is(Toys.Blindfold)) {
            return peers(Body.Blindfolded);
        } else if (item.is(Toys.Cock_Ring)) {
            return peers(Body.AroundCockBase);
        } else if (item.is(Toys.Anal_Douche)) {
            return peers(Body.InButt);
        } else if (item.is(Toys.Enema_Bulb)) {
            return peers(Body.InButt);
        } else if (item.is(Toys.Enema_Kit)) {
            return peers(Body.InButt);
        } else if (item.is(Toys.Glans_Ring)) {
            return peers(Body.OnPenis);
        } else if (item.is(Toys.Humbler)) {
            return peers(Body.OnBalls, Posture.CantStand, Posture.CantSitOnChair);
        } else if (item.is(Toys.Masturbator)) {
            return None;
        } else if (item.is(Toys.Pussy_Clamps)) {
            return peers(Body.OnLabia, Body.OnBalls);
        } else if (item.is(Toys.Clit_Clamp)) {
            return peers(Body.OnClit, Body.OnPenis);
        } else if (item.is(Toys.EStim_Device)) {
            return None;
        } else if (item.is(Toys.Doll)) {
            return None;
        } else if (item.is(Toys.Spouse)) {
            return None;
        } else {
            throw new IllegalArgumentException("Defaults not defined for " + item);
        }
    }

    private static final Enum<?>[] None = new Enum<?>[] {};

    private static Enum<?>[] peers(Enum<?>... state) {
        return state;
    }

    private static Enum<?>[] itemBlockers(QualifiedString state) {
        if (state.is(Toys.Buttplug)) {
            return blockedBy(Body.CrotchRoped);
        } else if (state.is(Toys.Cock_Ring)) {
            return blockedBy(Body.OnBalls, Body.OnPenis);
        } //
        else if (state.is(Clothes.Babydoll)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Blouse)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Body)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Bra)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Catsuit)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether, Body.WristsTied, Posture.WristsTiedBehindBack,
                    Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Dress)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Jacket)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Leotard)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Nightie)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Pajamas)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether, Body.WristsTied, Posture.WristsTiedBehindBack,
                    Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Pantyhose)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether);
        } else if (state.is(Clothes.Shirt)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Stockings)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether);
        } else if (state.is(Clothes.Suit)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Tanktop)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Trousers)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether);
        } else if (state.is(Clothes.Vest)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Underpants)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether);
        } //
        else if (state.is(Shoes.class)) {
            return blockedBy(Clothes.Socks, Clothes.Pantyhose, Clothes.Stockings);
        } else {
            return NotBlocked;
        }
    }

    private void applyPeerRules(QualifiedString kind, Collection<QualifiedString> defaultPeers,
            Collection<QualifiedString> itemAttributes, Collection<QualifiedString> itemBlockers) {
        for (QualifiedString peer : defaultPeers) {
            itemBlockers.addAll(QualifiedStringMapping.of(peerBlockers(peer)));
        }
    }

    private Enum<?>[] peerBlockers(QualifiedString peer) {
        if (peer.is(Body.InButt) || peer.is(Body.InVagina)) {
            return blockedBy(Body.CrotchRoped);
        } else if (peer.is(Body.AroundCockBase)) {
            return blockedBy(Body.OnBalls);
        } else {
            return NotBlocked;
        }
    }

    private static final Enum<?>[] NotBlocked = new Enum<?>[] {};

    private static Enum<?>[] blockedBy(Enum<?>... state) {
        return state;
    }

    private void relaxBlockingRules(QualifiedString kind, Collection<QualifiedString> defaultPeers,
            Collection<QualifiedString> itemAttributes, Collection<QualifiedString> itemBlockers) {
        if (kind.is(Toys.Cock_Ring)) {
            if (itemAttributes.contains(QualifiedString.of(Features.Detachable))) {
                itemBlockers.remove(QualifiedString.of(Body.OnBalls));
            }
        }
    }

}
