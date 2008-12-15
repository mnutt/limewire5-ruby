package org.limewire.promotion;

import org.limewire.inject.AbstractModule;
import org.limewire.promotion.impressions.ImpressionsCollector;
import org.limewire.promotion.impressions.ImpressionsCollectorImpl;

public class LimeWirePromotionModule extends AbstractModule {
    
    private final Class<? extends PromotionBinderRequestor> promotionBinderRequestorClass;
    private final Class<? extends PromotionServices> promotionServices;
    
    public LimeWirePromotionModule(Class<? extends PromotionBinderRequestor> promotionBinderRequestorClass,
            Class<? extends PromotionServices> promotionServices) {
        this.promotionBinderRequestorClass = promotionBinderRequestorClass;
        this.promotionServices = promotionServices;
    }

    @Override
    protected void configure() {
        bind(SearcherDatabase.class).to(SearcherDatabaseImpl.class);
        bind(PromotionSearcher.class).to(PromotionSearcherImpl.class);
        bind(PromotionBinderRepository.class).to(PromotionBinderRepositoryImpl.class);
        bind(KeywordUtil.class).to(KeywordUtilImpl.class);
        bind(ImpressionsCollector.class).to(ImpressionsCollectorImpl.class);
        bind(PromotionBinderFactory.class).to(PromotionBinderFactoryImpl.class);
        bind(PromotionBinderRequestor.class).to(promotionBinderRequestorClass);
        bind(PromotionServices.class).to(promotionServices);
    }
    
    

}
