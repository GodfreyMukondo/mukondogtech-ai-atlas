import React from "react";

export interface UsageMetrics {
  totalUsers: number;
  activeUsers: number;
  totalDocuments: number;
  documentsProcessedToday: number;
  totalStorageUsedGb: number;
  monthlyRevenue: number;
  aiRequestsToday: number;
  averageProcessingTimeSeconds: number;
}

interface AdminUsageStatsProps {
  metrics: UsageMetrics;
}

const StatCard = ({
  title,
  value,
}: {
  title: string;
  value: string | number;
}) => (
  <div className="rounded-xl border bg-white p-5 shadow-sm">
    <p className="text-sm text-gray-500">
      {title}
    </p>

    <h3 className="mt-2 text-2xl font-bold">
      {value}
    </h3>
  </div>
);

const AdminUsageStats: React.FC<
  AdminUsageStatsProps
> = ({ metrics }) => {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard
          title="Total Users"
          value={metrics.totalUsers}
        />

        <StatCard
          title="Active Users"
          value={metrics.activeUsers}
        />

        <StatCard
          title="Documents"
          value={metrics.totalDocuments}
        />

        <StatCard
          title="Documents Today"
          value={
            metrics.documentsProcessedToday
          }
        />

        <StatCard
          title="Storage Used"
          value={`${metrics.totalStorageUsedGb} GB`}
        />

        <StatCard
          title="Monthly Revenue"
          value={`$${metrics.monthlyRevenue.toLocaleString()}`}
        />

        <StatCard
          title="AI Requests Today"
          value={metrics.aiRequestsToday}
        />

        <StatCard
          title="Avg Processing Time"
          value={`${metrics.averageProcessingTimeSeconds}s`}
        />
      </div>

      <div className="rounded-xl border bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-semibold">
          Platform Health
        </h2>

        <div className="space-y-4">
          <div>
            <div className="mb-1 flex justify-between text-sm">
              <span>User Activity</span>
              <span>
                {Math.round(
                  (metrics.activeUsers /
                    Math.max(
                      metrics.totalUsers,
                      1
                    )) *
                    100
                )}
                %
              </span>
            </div>

            <div className="h-2 rounded bg-gray-200">
              <div
                className="h-2 rounded bg-green-500"
                style={{
                  width: `${
                    (metrics.activeUsers /
                      Math.max(
                        metrics.totalUsers,
                        1
                      )) *
                    100
                  }%`,
                }}
              />
            </div>
          </div>

          <div>
            <div className="mb-1 flex justify-between text-sm">
              <span>Document Volume</span>
              <span>
                {
                  metrics.documentsProcessedToday
                }{" "}
                Today
              </span>
            </div>

            <div className="h-2 rounded bg-gray-200">
              <div
                className="h-2 rounded bg-blue-500"
                style={{
                  width: `${Math.min(
                    metrics.documentsProcessedToday /
                      10,
                    100
                  )}%`,
                }}
              />
            </div>
          </div>

          <div>
            <div className="mb-1 flex justify-between text-sm">
              <span>AI Usage</span>
              <span>
                {metrics.aiRequestsToday}
              </span>
            </div>

            <div className="h-2 rounded bg-gray-200">
              <div
                className="h-2 rounded bg-purple-500"
                style={{
                  width: `${Math.min(
                    metrics.aiRequestsToday /
                      20,
                    100
                  )}%`,
                }}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminUsageStats;