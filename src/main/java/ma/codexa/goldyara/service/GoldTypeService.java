package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.dto.GoldTypeDTO;
import ma.codexa.goldyara.entity.GoldType;
import ma.codexa.goldyara.repository.GoldTypeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GoldTypeService {

    private final GoldTypeRepository goldTypeRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "goldTypes")
    public List<GoldTypeDTO> getAllGoldTypes() {
        log.debug("Fetching all gold types from database (cache miss)");
        return goldTypeRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<GoldTypeDTO> getByCode(String code) {
        return goldTypeRepository.findByCodeIgnoreCase(code).map(this::toDTO);
    }

    @CacheEvict(value = "goldTypes", allEntries = true)
    public GoldTypeDTO create(GoldTypeDTO dto) {
        GoldType entity = toEntity(dto);
        entity = goldTypeRepository.save(entity);
        log.info("Created gold type: {}", entity.getCode());
        return toDTO(entity);
    }

    @CacheEvict(value = "goldTypes", allEntries = true)
    public GoldTypeDTO update(Long id, GoldTypeDTO dto) {
        GoldType entity = goldTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type d'or", id));
        entity.setName(dto.getName());
        entity.setCode(dto.getCode() != null ? dto.getCode().toUpperCase() : entity.getCode());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entity = goldTypeRepository.save(entity);
        log.info("Updated gold type: {}", entity.getCode());
        return toDTO(entity);
    }

    @CacheEvict(value = "goldTypes", allEntries = true)
    public void delete(Long id) {
        if (!goldTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Type d'or", id);
        }
        goldTypeRepository.deleteById(id);
        log.info("Deleted gold type with id: {}", id);
    }

    private GoldTypeDTO toDTO(GoldType gt) {
        return new GoldTypeDTO(gt.getId(), gt.getName(), gt.getCode(), gt.getSortOrder());
    }

    private GoldType toEntity(GoldTypeDTO dto) {
        GoldType gt = new GoldType();
        gt.setName(dto.getName());
        gt.setCode(dto.getCode() != null ? dto.getCode().toUpperCase() : "");
        gt.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        return gt;
    }
}
