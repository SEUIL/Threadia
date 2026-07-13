export type IssueStatus = "BACKLOG" | "TODO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE";

/** Issue API가 보드에 제공하는 최소 이슈 정보. */
export interface IssueResponse {
  id: number;
  issueKey: string;
  title: string;
  status: IssueStatus;
}

export interface IssuesData {
  issues: IssueResponse[];
}

export interface IssueData {
  issue: IssueResponse;
}
