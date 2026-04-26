package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.dto.PromoModalDTO;
import ma.codexa.goldyara.entity.PromoModal;
import ma.codexa.goldyara.repository.PromoModalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PromoModalService {
    
    private final PromoModalRepository promoModalRepository;
    
    public List<PromoModalDTO> getAllPromoModals() {
        List<PromoModal> modals = promoModalRepository.findAll();
        return modals.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<PromoModalDTO> getActivePromoModals() {
        List<PromoModal> modals = promoModalRepository.findByIsActiveOrderByDisplayOrderAsc(true);
        return modals.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public PromoModalDTO getFirstActivePromoModal() {
        return promoModalRepository.findFirstByIsActiveOrderByDisplayOrderAsc(true)
                .map(this::convertToDTO)
                .orElse(null);
    }
    
    public PromoModalDTO getPromoModalById(Long id) {
        PromoModal modal = promoModalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromoModal", id));
        return convertToDTO(modal);
    }
    
    public PromoModalDTO createPromoModal(PromoModalDTO modalDTO) {
        PromoModal modal = convertToEntity(modalDTO);
        PromoModal savedModal = promoModalRepository.save(modal);
        log.info("Created promo modal: {}", savedModal.getId());
        return convertToDTO(savedModal);
    }
    
    public PromoModalDTO updatePromoModal(Long id, PromoModalDTO modalDTO) {
        PromoModal existingModal = promoModalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromoModal", id));
        
        existingModal.setTitle(modalDTO.getTitle());
        existingModal.setDescription(modalDTO.getDescription());
        existingModal.setImageUrl(modalDTO.getImageUrl());
        existingModal.setButtonText(modalDTO.getButtonText());
        existingModal.setButtonUrl(modalDTO.getButtonUrl());
        existingModal.setAutoCloseSeconds(modalDTO.getAutoCloseSeconds());
        existingModal.setIsActive(modalDTO.getIsActive());
        existingModal.setDisplayOrder(modalDTO.getDisplayOrder());
        
        PromoModal updatedModal = promoModalRepository.save(existingModal);
        log.info("Updated promo modal: {}", updatedModal.getId());
        return convertToDTO(updatedModal);
    }
    
    public void deletePromoModal(Long id) {
        if (!promoModalRepository.existsById(id)) {
            throw new ResourceNotFoundException("PromoModal", id);
        }
        promoModalRepository.deleteById(id);
        log.info("Deleted promo modal: {}", id);
    }
    
    public PromoModalDTO toggleActive(Long id, Boolean isActive) {
        PromoModal modal = promoModalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromoModal", id));
        
        modal.setIsActive(isActive);
        PromoModal updatedModal = promoModalRepository.save(modal);
        log.info("Toggled promo modal {} active status to: {}", id, isActive);
        return convertToDTO(updatedModal);
    }
    
    private PromoModalDTO convertToDTO(PromoModal modal) {
        PromoModalDTO dto = new PromoModalDTO();
        dto.setId(modal.getId());
        dto.setTitle(modal.getTitle());
        dto.setDescription(modal.getDescription());
        dto.setImageUrl(modal.getImageUrl());
        dto.setButtonText(modal.getButtonText());
        dto.setButtonUrl(modal.getButtonUrl());
        dto.setAutoCloseSeconds(modal.getAutoCloseSeconds());
        dto.setIsActive(modal.getIsActive());
        dto.setDisplayOrder(modal.getDisplayOrder());
        return dto;
    }
    
    private PromoModal convertToEntity(PromoModalDTO dto) {
        PromoModal modal = new PromoModal();
        modal.setTitle(dto.getTitle());
        modal.setDescription(dto.getDescription());
        modal.setImageUrl(dto.getImageUrl());
        modal.setButtonText(dto.getButtonText());
        modal.setButtonUrl(dto.getButtonUrl());
        modal.setAutoCloseSeconds(dto.getAutoCloseSeconds());
        modal.setIsActive(dto.getIsActive());
        modal.setDisplayOrder(dto.getDisplayOrder());
        return modal;
    }
}
