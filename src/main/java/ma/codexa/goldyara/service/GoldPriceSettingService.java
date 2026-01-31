package ma.codexa.goldyara.service;

import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.entity.GoldPriceSetting;
import ma.codexa.goldyara.repository.GoldPriceSettingRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class GoldPriceSettingService {

    private static final double DEFAULT_PRICE_PER_GRAM = 650.0;

    private final GoldPriceSettingRepository goldPriceSettingRepository;
    private final ProductService productService;

    public GoldPriceSettingService(
            GoldPriceSettingRepository goldPriceSettingRepository,
            @Lazy ProductService productService) {
        this.goldPriceSettingRepository = goldPriceSettingRepository;
        this.productService = productService;
    }

    /**
     * Retourne les paramètres (prix au gramme uniquement). La marge est par produit.
     */
    public GoldPriceSetting getSettings() {
        return goldPriceSettingRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefault);
    }

    /**
     * Calcule le prix : (grammes × prix_par_gramme) + marginGain_du_produit
     */
    public double calculatePrice(double weightInGrams, double marginGain) {
        GoldPriceSetting s = getSettings();
        return weightInGrams * s.getPricePerGram() + marginGain;
    }

    @Transactional
    public GoldPriceSetting updateSettings(double pricePerGram) {
        GoldPriceSetting s = getSettings();
        s.setPricePerGram(pricePerGram);
        GoldPriceSetting saved = goldPriceSettingRepository.save(s);
        productService.updateAllPricesForNewGoldRate(pricePerGram);
        log.info("Prix au gramme mis à jour à {} MAD - tous les produits recalculés", pricePerGram);
        return saved;
    }

    private GoldPriceSetting createDefault() {
        GoldPriceSetting s = new GoldPriceSetting();
        s.setPricePerGram(DEFAULT_PRICE_PER_GRAM);
        return goldPriceSettingRepository.save(s);
    }
}
