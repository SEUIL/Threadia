/*
 * 정수 시드로부터 결정적 의사난수를 만든다. invitation.id처럼 안정적인 정수를
 * 시드로 넣으면 리렌더가 일어나도 항상 같은 값이 나온다 — 봉투 더미의 회전각/
 * 오프셋에 Math.random()을 쓰면 리렌더마다 봉투가 흔들리는 문제를 막기 위함.
 * channel은 같은 seed에서 서로 독립적인 값(회전, 오프셋 등)을 뽑을 때 쓴다.
 */
function hash(seed: number, channel: number): number {
  let h = (seed * 2654435761 + channel * 40503) | 0;
  h = Math.imul(h ^ (h >>> 16), 2246822507);
  h = Math.imul(h ^ (h >>> 13), 3266489909);
  h ^= h >>> 16;
  return h >>> 0;
}

/** seed(+channel)로부터 [-1, 1) 범위 결정적 의사난수를 반환한다. */
export function seededRandom(seed: number, channel = 0): number {
  return (hash(seed, channel) / 0xffffffff) * 2 - 1;
}
