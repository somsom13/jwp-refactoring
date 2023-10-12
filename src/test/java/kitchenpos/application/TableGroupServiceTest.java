package kitchenpos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.dao.TableGroupDao;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.TableGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableGroupServiceTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private OrderTableDao orderTableDao;

    @Mock
    private TableGroupDao tableGroupDao;

    @InjectMocks
    private TableGroupService tableGroupService;

    @DisplayName("테이블 그룹을 생성, 저장한다.")
    @Test
    void create_success() {
        // given
        final TableGroup tableGroup = new TableGroup();
        final OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        orderTable1.setTableGroupId(null);
        orderTable1.setEmpty(true);

        final OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);
        orderTable2.setTableGroupId(null);
        orderTable2.setEmpty(true);

        tableGroup.setOrderTables(List.of(orderTable1, orderTable2));
        tableGroup.setId(1L);
        tableGroup.setCreatedDate(LocalDateTime.now());

        given(orderTableDao.findAllByIdIn(anyList()))
            .willReturn(List.of(orderTable1, orderTable2));
        given(tableGroupDao.save(any(TableGroup.class)))
            .willReturn(tableGroup);
        given(orderTableDao.save(any(OrderTable.class)))
            .willReturn(orderTable1)
            .willReturn(orderTable2);

        // when
        final TableGroup savedTableGroup = tableGroupService.create(tableGroup);

        // then
        assertThat(savedTableGroup).usingRecursiveComparison()
            .isEqualTo(tableGroup);
    }

    @DisplayName("테이블 그룹을 생성할 때, 묶을 테이블이 없으면 예외가 발생한다.")
    @Test
    void create_empty_fail() {
        // given
        final TableGroup tableGroup = new TableGroup();

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("테이블 그룹을 생성할 때, 테이블이 2개 보다 적다면 예외가 발생한다.")
    @Test
    void create_wrongSize_fail() {
        // given
        final TableGroup tableGroup = new TableGroup();
        tableGroup.setOrderTables(List.of(new OrderTable()));

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("테이블 그룹을 생성할 때, 존재하지 않는 테이블이 있다면 예외가 발생한다.")
    @Test
    void create_notExistOrderTable_fail() {
        // given
        final TableGroup tableGroup = new TableGroup();
        final OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        final OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);
        tableGroup.setOrderTables(List.of(orderTable1, orderTable2));

        given(orderTableDao.findAllByIdIn(anyList()))
            .willReturn(List.of(orderTable1));

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("테이블 그룹을 생성할 때, 비어있지 않은 테이블이 있다면 예외가 발생한다.")
    @Test
    void create_emptyTable_fail() {
        // given
        final TableGroup tableGroup = new TableGroup();
        tableGroup.setId(1L);
        final OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        orderTable1.setEmpty(false);
        orderTable1.setTableGroupId(null);

        final OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);
        orderTable1.setEmpty(false);
        orderTable1.setTableGroupId(null);

        tableGroup.setOrderTables(List.of(orderTable1, orderTable2));

        given(orderTableDao.findAllByIdIn(anyList()))
            .willReturn(List.of(orderTable1, orderTable2));

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("테이블 그룹을 생성할 때, 이미 다른 그룹에 속한 테이블이 있다면 예외가 발생한다.")
    @Test
    void create_alreadyInOtherGroup_fail() {
        // given
        final TableGroup tableGroup = new TableGroup();
        tableGroup.setId(1L);
        final OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        orderTable1.setEmpty(true);
        orderTable1.setTableGroupId(2L);

        final OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);
        orderTable1.setEmpty(true);
        orderTable1.setTableGroupId(null);

        tableGroup.setOrderTables(List.of(orderTable1, orderTable2));

        given(orderTableDao.findAllByIdIn(anyList()))
            .willReturn(List.of(orderTable1, orderTable2));

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("그룹을 해제한다.")
    @Test
    void ungroup_success() {
        // given
        final TableGroup tableGroup = new TableGroup();
        tableGroup.setId(1L);

        final OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        orderTable1.setTableGroupId(tableGroup.getId());
        orderTable1.setEmpty(false);

        final OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);
        orderTable2.setTableGroupId(tableGroup.getId());
        orderTable2.setEmpty(false);

        tableGroup.setOrderTables(List.of(orderTable1, orderTable2));
        tableGroup.setCreatedDate(LocalDateTime.now());

        given(orderTableDao.findAllByTableGroupId(anyLong()))
            .willReturn(List.of(orderTable1, orderTable2));
        given(orderDao.existsByOrderTableIdInAndOrderStatusIn(anyList(), anyList()))
            .willReturn(false);
        given(orderTableDao.save(any(OrderTable.class)))
            .willReturn(orderTable1, orderTable2);

        // when, then
        assertDoesNotThrow(() -> tableGroupService.ungroup(tableGroup.getId()));
    }

    @DisplayName("그룹에 속한 테이블 중, 주문 상태가 COMPLETION 인 테이블이 있다면 예외가 발생한다.")
    @Test
    void ungroup_wrongStatus_fail() {
        // given
        final TableGroup tableGroup = new TableGroup();
        tableGroup.setId(1L);

        final OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        final OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);

        tableGroup.setOrderTables(List.of(orderTable1, orderTable2));
        tableGroup.setCreatedDate(LocalDateTime.now());

        given(orderTableDao.findAllByTableGroupId(anyLong()))
            .willReturn(List.of(orderTable1, orderTable2));
        given(orderDao.existsByOrderTableIdInAndOrderStatusIn(anyList(), anyList()))
            .willReturn(true);

        // when, then
        final Long groupId = tableGroup.getId();

        assertThatThrownBy(() -> tableGroupService.ungroup(groupId))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
