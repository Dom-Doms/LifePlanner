package it.univ.lifeplanner.planning.service;

import it.univ.lifeplanner.common.ForbiddenException;
import it.univ.lifeplanner.common.NotFoundException;
import it.univ.lifeplanner.planning.dto.DayContextRequest;
import it.univ.lifeplanner.planning.dto.DayContextResponse;
import it.univ.lifeplanner.planning.model.DayContext;
import it.univ.lifeplanner.planning.repository.DayContextRepository;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.service.CurrentUserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DayContextService {
    private final DayContextRepository repository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<DayContextResponse> findAllForCurrentUser() {
        AppUser user = currentUserService.requireCurrentUser();
        return repository.findByOwnerOrderByLabelAsc(user).stream().map(DayContextResponse::from).toList();
    }

    @Transactional
    public DayContextResponse create(DayContextRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        DayContext context = new DayContext();
        context.setOwner(user);
        apply(context, request);
        return DayContextResponse.from(repository.save(context));
    }

    @Transactional
    public DayContextResponse update(Long id, DayContextRequest request) {
        DayContext context = requireOwned(id);
        apply(context, request);
        return DayContextResponse.from(context);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireOwned(id));
    }

    public DayContext requireOwnedEntity(Long id) {
        return requireOwned(id);
    }

    private DayContext requireOwned(Long id) {
        AppUser user = currentUserService.requireCurrentUser();
        DayContext context = repository.findById(id).orElseThrow(() -> new NotFoundException("Day context not found"));
        if (!currentUserService.isAdmin(user) && !context.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot access another user's day context");
        }
        return context;
    }

    private void apply(DayContext context, DayContextRequest request) {
        context.setLabel(request.label().trim());
        context.setColor(request.color());
        context.setEmoji(request.emoji());
        context.setActive(request.active() == null || request.active());
    }
}
