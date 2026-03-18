package com.equityseer.service.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.repository.stock.StockOHLCVRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockOHLCVServiceImplTest {
  @Mock private StockOHLCVRepository repository;

  @InjectMocks private StockOHLCVServiceImpl service;

  @Captor private ArgumentCaptor<StockOHLCV> ohlcvCaptor;

  private static StockOHLCV valid(String symbol, LocalDate date) {
    var o = new StockOHLCV();
    o.setSymbol(symbol);
    o.setDate(date);
    o.setOpen(new BigDecimal("100.00"));
    o.setHigh(new BigDecimal("110.00"));
    o.setLow(new BigDecimal("90.00"));
    o.setClose(new BigDecimal("105.00"));
    o.setVolume(1000L);
    return o;
  }

  @Test
  void save_throwsWhenNull() {
    assertThatThrownBy(() -> service.save(null)).isInstanceOf(NullPointerException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void save_throwsWhenHighLessThanLow() {
    var o = valid("TCS", LocalDate.of(2026, 1, 1));
    o.setHigh(new BigDecimal("10.00"));
    o.setLow(new BigDecimal("11.00"));

    assertThatThrownBy(() -> service.save(o))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("high must be >=");
    verify(repository, never()).save(any());
  }

  @Test
  void save_throwsWhenVolumeNotPositive() {
    var o = valid("TCS", LocalDate.of(2026, 1, 1));
    o.setVolume(0L);

    assertThatThrownBy(() -> service.save(o))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("volume must be positive");
    verify(repository, never()).save(any());
  }

  @Test
  void saveAll_validatesAllRowsAndDelegatesToRepository() {
    var o1 = valid("TCS", LocalDate.of(2026, 1, 1));
    var o2 = valid("TCS", LocalDate.of(2026, 1, 2));

    when(repository.saveAll(List.of(o1, o2))).thenReturn(List.of(o1, o2));

    var result = service.saveAll(List.of(o1, o2));

    assertThat(result).hasSize(2);
    verify(repository).saveAll(List.of(o1, o2));
  }

  @Test
  void findBySymbolAndDateRange_throwsWhenEndBeforeStart() {
    assertThatThrownBy(
            () ->
                service.findBySymbolAndDateRange(
                    "TCS", LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("end must be >=");
  }

  @Test
  void findLatestBySymbol_returnsEmptyWhenRepositoryReturnsNull() {
    when(repository.findFirstBySymbolOrderByDateDesc("TCS")).thenReturn(null);

    assertThat(service.findLatestBySymbol("TCS")).isEmpty();
  }

  @Test
  void upsert_insertsWhenMissing() {
    var o = valid("TCS", LocalDate.of(2026, 1, 1));

    when(repository.findBySymbolAndDate("TCS", LocalDate.of(2026, 1, 1)))
        .thenReturn(Optional.empty());
    when(repository.save(o)).thenReturn(o);

    var result = service.upsert(o);

    assertThat(result).isSameAs(o);
    verify(repository).save(o);
  }

  @Test
  void upsert_updatesExistingWhenPresent() {
    var incoming = valid("TCS", LocalDate.of(2026, 1, 1));
    incoming.setOpen(new BigDecimal("101.00"));
    incoming.setHigh(new BigDecimal("111.00"));
    incoming.setLow(new BigDecimal("91.00"));
    incoming.setClose(new BigDecimal("106.00"));
    incoming.setVolume(2000L);

    var existing = valid("TCS", LocalDate.of(2026, 1, 1));
    existing.setId(99L);

    when(repository.findBySymbolAndDate("TCS", LocalDate.of(2026, 1, 1)))
        .thenReturn(Optional.of(existing));
    when(repository.save(any(StockOHLCV.class))).thenAnswer(inv -> inv.getArgument(0));

    var result = service.upsert(incoming);

    assertThat(result.getId()).isEqualTo(99L);
    assertThat(result.getOpen()).isEqualByComparingTo("101.00");
    assertThat(result.getHigh()).isEqualByComparingTo("111.00");
    assertThat(result.getLow()).isEqualByComparingTo("91.00");
    assertThat(result.getClose()).isEqualByComparingTo("106.00");
    assertThat(result.getVolume()).isEqualTo(2000L);

    verify(repository).save(ohlcvCaptor.capture());
    assertThat(ohlcvCaptor.getValue().getId()).isEqualTo(99L);
  }
}
