/*
 * Copyright (C) 2017 The Katz Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package katz

class CoproductComonad<F, G> : Comonad<CoproductFG<F, G>>, GlobalInstance<Comonad<CoproductFG<F, G>>>() {

    override fun <A, B> coflatMap(fa: CoproductKind<F, G, A>, f: (CoproductKind<F, G, A>) -> B): Coproduct<F, G, B> =
            fa.ev().coflatMap(f)

    override fun <A> extract(fa: CoproductKind<F, G, A>): A =
            fa.ev().extract()

    override fun <A, B> map(fa: HK<CoproductFG<F, G>, A>, f: (A) -> B): HK<CoproductFG<F, G>, B> = fa.ev().map(f)

}
