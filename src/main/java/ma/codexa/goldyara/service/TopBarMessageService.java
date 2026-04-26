package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.dto.TopBarMessageDTO;
import ma.codexa.goldyara.entity.TopBarMessage;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.repository.TopBarMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TopBarMessageService {
    
    private final TopBarMessageRepository topBarMessageRepository;
    
    public List<TopBarMessageDTO> getAllActiveMessages() {
        log.info("Récupération de tous les messages actifs de la top bar");
        return topBarMessageRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<TopBarMessageDTO> getAllMessages() {
        log.info("Récupération de tous les messages de la top bar");
        return topBarMessageRepository.findByIsActiveOrderByDisplayOrderAsc(null)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public TopBarMessageDTO createMessage(TopBarMessageDTO messageDTO) {
        log.info("Création d'un nouveau message de top bar: {}", messageDTO.getMessage());
        
        TopBarMessage message = new TopBarMessage();
        message.setMessage(messageDTO.getMessage());
        message.setDisplayOrder(messageDTO.getDisplayOrder());
        message.setIsActive(messageDTO.getIsActive() != null ? messageDTO.getIsActive() : true);
        
        TopBarMessage savedMessage = topBarMessageRepository.save(message);
        log.info("Message créé avec l'ID: {}", savedMessage.getId());
        
        return convertToDTO(savedMessage);
    }
    
    public TopBarMessageDTO updateMessage(Long id, TopBarMessageDTO messageDTO) {
        log.info("Mise à jour du message de top bar avec l'ID: {}", id);
        
        TopBarMessage existingMessage = topBarMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message de top bar", id));
        
        existingMessage.setMessage(messageDTO.getMessage());
        existingMessage.setDisplayOrder(messageDTO.getDisplayOrder());
        if (messageDTO.getIsActive() != null) {
            existingMessage.setIsActive(messageDTO.getIsActive());
        }
        
        TopBarMessage updatedMessage = topBarMessageRepository.save(existingMessage);
        log.info("Message mis à jour avec succès");
        
        return convertToDTO(updatedMessage);
    }
    
    public void deleteMessage(Long id) {
        log.info("Suppression du message de top bar avec l'ID: {}", id);
        
        TopBarMessage message = topBarMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message de top bar", id));
        
        topBarMessageRepository.delete(message);
        log.info("Message supprimé avec succès");
    }
    
    public TopBarMessageDTO toggleActive(Long id, Boolean isActive) {
        log.info("Changement du statut actif du message {} à {}", id, isActive);
        
        TopBarMessage message = topBarMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message de top bar", id));
        
        message.setIsActive(isActive);
        TopBarMessage updatedMessage = topBarMessageRepository.save(message);
        
        return convertToDTO(updatedMessage);
    }
    
    private TopBarMessageDTO convertToDTO(TopBarMessage message) {
        TopBarMessageDTO dto = new TopBarMessageDTO();
        dto.setId(message.getId());
        dto.setMessage(message.getMessage());
        dto.setDisplayOrder(message.getDisplayOrder());
        dto.setIsActive(message.getIsActive());
        return dto;
    }
}
