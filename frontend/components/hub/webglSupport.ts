/*
 * WebGL 지원 여부를 사전에 확인하는 헬퍼.
 *
 * PR #62 QA(WBS 3.2.5, 황스일)에서 WebGL context 생성이 실패하는 환경에서
 * `/hub`가 fallback 없이 깨지는 걸 발견했다(`THREE.WebGLRenderer: A WebGL
 * context could not be created.`) — `<Canvas>`를 무조건 렌더링해서 생긴
 * 문제라, 마운트 전에 이 함수로 먼저 확인하고 실패 시 안내 화면으로 대체한다.
 */
export function isWebGLAvailable(): boolean {
  if (typeof window === "undefined") return false;
  try {
    const canvas = document.createElement("canvas");
    const gl = canvas.getContext("webgl2") ?? canvas.getContext("webgl") ?? canvas.getContext("experimental-webgl");
    return !!gl;
  } catch {
    return false;
  }
}
