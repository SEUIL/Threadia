"use client";

/*
 * `<Canvas>` 마운트 중(WebGL context 생성 등) 발생하는 런타임 에러를 잡아
 * 안내 화면으로 대체하는 error boundary. 사전 체크(`webglSupport.ts`)로
 * 대부분 걸러지지만, context가 마운트 후 유실되는 경우 등 나머지 실패를
 * 여기서 한 번 더 막는다.
 */
import { Component, type ReactNode } from "react";

type Props = { fallback: ReactNode; children: ReactNode };
type State = { hasError: boolean };

export class HubSceneErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: unknown) {
    console.error("[HubSceneErrorBoundary] 3D 씬 렌더링 실패:", error);
  }

  render() {
    return this.state.hasError ? this.props.fallback : this.props.children;
  }
}
