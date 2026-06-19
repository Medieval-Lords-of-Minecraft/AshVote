package me.neoblade298.ashvote.sites;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SiteManager {

    private final Map<String, VoteSite> sitesById = new HashMap<>();
    private final Map<String, VoteSite> sitesByService = new HashMap<>();

    public void clear() {
        sitesById.clear();
        sitesByService.clear();
    }

    public void register(VoteSite site) {
        sitesById.put(site.getId(), site);
        sitesByService.put(site.getServiceName().toLowerCase(), site);
    }

    public VoteSite getById(String id) {
        return sitesById.get(id);
    }

    public VoteSite getByService(String serviceName) {
        return sitesByService.get(serviceName.toLowerCase());
    }

    public Collection<VoteSite> getAll() {
        return sitesById.values();
    }
}
