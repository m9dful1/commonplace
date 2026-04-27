import { FragmentForm } from "@/components/FragmentForm";
import { FragmentList } from "@/components/FragmentList";
import { listFragments } from "@/lib/fragments";

export const dynamic = "force-dynamic";

export default function HomePage() {
  const fragments = listFragments();
  return (
    <>
      <FragmentForm />
      <FragmentList fragments={fragments} />
    </>
  );
}
