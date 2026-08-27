package com.example.ssds.api.scoring;

import com.example.ssds.core.domain.SceneType;
import com.example.ssds.infra.entity.Product;

/**
 * 情境原型判定（規格書 §5.10、Agent 1 SceneClassifierAgent，§6.3）。
 *
 * <p>情境判定本身是 Track 3（{@code ssds-ai}）的職責——依品項熱度/銷售特徵挑選
 * VIRAL／FESTIVAL／REPLENISHMENT／SEASONAL 之一。Track 3 尚未實作，
 * {@link ProductScoringOrchestrator} 因此把「怎麼決定情境」抽成這個介面，
 * 不內建任何假的 LLM 呼叫或寫死的規則。Track 3 完工後應提供一個
 * 呼叫 SceneClassifierAgent 的實作並取代呼叫端目前傳入的任何 stub／固定值。
 */
@FunctionalInterface
public interface SceneResolver {
    SceneType resolve(Product product);
}
