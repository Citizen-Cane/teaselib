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

import teaselib.core.state.AbstractProxy;
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

    private final TeaseLib teaseLib;
    private final UserItemsLogic rules = new UserItemsLogic();
    private final Map<String, ItemMap> domainMap = new HashMap<>();
    List<URL> userItemDefinitions = new ArrayList<>();

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
        userItemDefinitions.add(url);
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
            userItemDefinitions.add(url);
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
        List<ItemImpl> instances = items.stream().map(AbstractProxy::removeProxy).map(ItemImpl.class::cast).toList();
        addItems(itemMap, instances);
    }

    public void clearCachedItems() {
        domainMap.clear();
    }

    public void clearLoadOrder() {
        userItemDefinitions.clear();
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
        Set<QualifiedString> itemAttributes = new HashSet<>();
        Set<QualifiedString> defaultPeers = QualifiedStringMapping.of(rules.defaultPeers(kind));
        Set<QualifiedString> itemBlockers = QualifiedStringMapping.of(//
                rules.physicallyBlockingPeers(kind), rules.logicallyBlockingPeers(kind));

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
                    itemBlockers.add(QualifiedString.of(name));
                }
            }
        }
        rules.applyPeerRules(kind, defaultPeers, itemAttributes, itemBlockers);
        rules.relaxBlockingRules(kind, defaultPeers, itemAttributes, itemBlockers);

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
        for (var def : userItemDefinitions) {
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

}
