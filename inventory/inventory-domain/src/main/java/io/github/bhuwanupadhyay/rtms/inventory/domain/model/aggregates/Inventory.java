package io.github.bhuwanupadhyay.rtms.inventory.domain.model.aggregates;

import io.github.bhuwanupadhyay.rtms.command.WorkflowCommand;
import io.github.bhuwanupadhyay.rtms.ddd.*;
import io.github.bhuwanupadhyay.rtms.inventory.domain.events.WorkflowExecuted;
import io.github.bhuwanupadhyay.rtms.inventory.domain.model.InventoryDb;
import io.github.bhuwanupadhyay.rtms.inventory.domain.model.valueobjects.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("ConstantConditions")
@Entity
@Table(name = InventoryDb.TABLE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.PRIVATE)
@Getter
@Slf4j
public class Inventory extends AggregateRoot<InventoryId> {

  @Embedded private InventoryName inventoryName;

  @ElementCollection(fetch = FetchType.EAGER)
  private Set<ProductLine> productLines;

  @Enumerated(EnumType.STRING)
  @Column(name = InventoryDb.STATUS)
  private InventoryStatus status;

  @ElementCollection(fetch = FetchType.LAZY)
  private List<UserComment> userComments;

  public Inventory(InventoryId inventoryId) {
    super(inventoryId);
  }

  public Result<Inventory> execute(WorkflowCommand command) {
    log.debug("Params => {}", command);

    List<DomainError> errors = new ArrayList<>();

    DomainAsserts.begin(command)
        .notNull(DomainError.create(this, "InventoryWorkflowCommandIsRequired"))
        .switchIfNotNull(
            Optional.ofNullable(command).map(WorkflowCommand::getAction),
            DomainError.create(this, "WorkflowActionIsRequired"))
        .notBlank(DomainError.create(this, "WorkflowActionIsRequired"))
        .switchIfNotNull(
            Optional.ofNullable(command).map(WorkflowCommand::getComment),
            DomainError.create(this, "WorkflowActionCommentIsRequired"))
        .notBlank(DomainError.create(this, "WorkflowActionCommentIsRequired"))
        .end();

    if (!this.getStatus().getNextActions().contains(command.getAction())) {
      throw new DomainException(List.of(DomainError.create(this, "WorkflowActionIsInvalid").get()));
    }

    this.userComments = Optional.ofNullable(this.userComments).orElseGet(ArrayList::new);
    this.userComments.add(new UserComment("SYSTEM", command.getAction(), command.getComment()));

    if (Actions.REPAIR.equals(command.getAction())) {
      DomainAsserts.begin(command.getPayloadJson())
          .notBlank(DomainError.create(this, "WorkflowPayloadJsonIsRequiredForRepair"))
          .end();
      String payload = command.getPayloadJson();
    }

    this.status = this.getStatus().nextStatus(command.getAction());
    this.registerEvent(new WorkflowExecuted(command.getAction(), this.getStatus().name()));
    log.debug("Executed {} {}", command.getClass().getName(), command);

    return Result.<Inventory>builder().result(this).domainErrors(errors).build();
  }
}