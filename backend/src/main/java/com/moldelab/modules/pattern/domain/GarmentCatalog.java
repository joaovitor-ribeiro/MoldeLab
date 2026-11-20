package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.pattern.drafting.CircleSkirtDrafter;
import com.moldelab.modules.pattern.drafting.StraightSkirtDrafter;
import com.moldelab.modules.pattern.drafting.TShirtDrafter;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registro dos modelos disponíveis. Para adicionar um modelo: novo GarmentType + GarmentDrafter aqui. */
public final class GarmentCatalog {

    private final Map<GarmentType, GarmentDrafter> drafters = new EnumMap<>(GarmentType.class);

    public GarmentCatalog() {
        this(List.of(new StraightSkirtDrafter(), new CircleSkirtDrafter(), new TShirtDrafter()));
    }

    public GarmentCatalog(List<GarmentDrafter> list) {
        list.forEach(d -> drafters.put(d.type(), d));
    }

    public Optional<GarmentDrafter> find(GarmentType type) {
        return Optional.ofNullable(drafters.get(type));
    }

    public List<GarmentDrafter> all() {
        return List.copyOf(drafters.values());
    }
}
