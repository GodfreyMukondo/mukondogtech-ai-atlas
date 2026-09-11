/**
 * ============================================================================
 * NOTIFICATION TYPES
 * ============================================================================
 */

export interface Notification {
  id: number;
  type: string;
  title: string;
  message: string | null;
  link: string | null;
  read: boolean;
  createdAt: string;
}
